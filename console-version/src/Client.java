import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Scanner;

public class Client implements Runnable{
    private String clientName = null;
    private Socket socket = null;
    
    private Scanner inputStream = null;
    private PrintWriter outputStream = null;
    private BufferedReader response = null;
    public static Encryption encryption;
    private final String key;

    private String public_key_string;
    private final String private_key_string;

    public Client(String hostName, int portNumber, String clientName) {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            key=properties.getProperty("key");
            public_key_string=properties.getProperty("public_key_string");
            private_key_string=properties.getProperty("private_key_string");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.clientName = clientName;
        while (true) {
            try {
                socket = new Socket(hostName, portNumber);
                System.out.println("you're now connected!");
                inputStream = new Scanner(System.in);
                outputStream = new PrintWriter(socket.getOutputStream());
                response=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                int n= Integer.parseInt(response.readLine());
                if(n==1){
                    Server.encryption=new RSA();
                }
                else
                    Server.encryption=new AES_Enryption();
                outputStream.println(clientName);
                outputStream.flush();
                break;
            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
    @Override
    public void run() {
        //Sender
        Thread sender = getSender();
        try { //Get the response
            response = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //Receiver
        Thread receiver = getReceiver();
        sender.start();
        receiver.start();
        try {
            //Wait until sender & receiver thread is done
            sender.join();
            receiver.join();
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public Thread getSender() {
        return new Thread(() -> {
            String line = "";
            while (true) {
                String K1=(Server.encryption instanceof RSA? public_key_string : key);
                line = inputStream.nextLine();
                if(line==null)
                    continue;
                try {
                    if (!line.isBlank())
                        // line = aes.encrpytMsg(line, key, IV);
                        line = Server.encryption.encryptMsg(line, K1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                outputStream.println(line);
                outputStream.flush(); //Prevents buffering of the message. Sends out the message immediately
            }
        });
    }
    public Thread getReceiver() {
        return new Thread(() -> {
            String line = "";
            while (true) {
                String K2=(Server.encryption instanceof RSA? private_key_string : key);
                try {
                    line = response.readLine();
                    if (line == null)
                        break;
                    if (!line.isBlank())
                        //line = aes.decryptMsg(line, key, IV);
                       line = Server.encryption.decryptMsg(line, K2);
                    System.out.println(line);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                    break;
                }
            }
        });
    }
    public static void main(String[] args) {

        String userName = null;
        Scanner in = new Scanner(System.in);
        System.out.println("Pls, enter your name here: ");
        while (true) {
            userName = in.nextLine();
            if (userName.isBlank()) {
                System.out.println("Name Cannot be blank");
                continue;
            }
            if (findClient(userName)) {
                System.out.println("Name Found already");
                continue;
            }
            break;
        }
        String portNumber = "8888";
        System.out.println(portNumber);
        in.reset();
        int port = 8888;
        try {
            Thread cl = new Thread(new Client("localhost", port,userName));
            cl.start();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }
    }
    private static boolean findClient(String userName) {
        for (int i = 0; i < CommunicationHandler.clients.size(); i++) {
            if (CommunicationHandler.clients.get(i).name.equals(userName)) {
                return true;
            }
        }
        return false;
    }
    public String getClientName() {
        return clientName;
    }
}