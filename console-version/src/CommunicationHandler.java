import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.*;


public class CommunicationHandler implements Runnable {

    public String name = null;
    public BufferedReader clientInputStream;
    public PrintWriter clientOutputStream;
    private Socket clientSocket;
    public  static List<CommunicationHandler> clients=new ArrayList<>();
    private CommunicationHandler client2=null;
    private final String key;
    AES_Enryption aes = new AES_Enryption();
    RSA rsa = new RSA();
    private final String public_key_string;
    private final String private_key_string;

    public CommunicationHandler(Socket clientSocket) {
        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        key=properties.getProperty("key");
        public_key_string=properties.getProperty("public_key_string");
        private_key_string=properties.getProperty("private_key_string");
        this.clientSocket = clientSocket;
        try {
            this.clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.clientOutputStream = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            this.clientOutputStream.println((Server.encryption instanceof RSA ? "1" : "2"));
            this.clientOutputStream.flush();
            this.name = clientInputStream.readLine();
            clients.add(this);
            System.out.println(clients);
            System.out.println(Server.encryption);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    @Override
    public void run() {
        String K2=(Server.encryption instanceof RSA? private_key_string : key);
        String K1=(Server.encryption instanceof RSA? public_key_string : key);
        String inputMessageFromClient = "", recipientName = "", messageToReceiver = "";
        while (true) {
            try {
                if (!clientInputStream.ready())
                    continue;
                inputMessageFromClient = clientInputStream.readLine();
                if(!inputMessageFromClient.isBlank()){
                    inputMessageFromClient= Server.encryption.decryptMsg(inputMessageFromClient, K2);
                    }
                else
                    continue;
                if (client2 == null) {
                    try {
                        findClient(inputMessageFromClient);
                        clientOutputStream.println(Server.encryption.encryptMsg("Connected Successfully With " + client2.name,K1));
                        clientOutputStream.flush();
                    } catch (Exception ex) {
                        clientOutputStream.println(Server.encryption.encryptMsg("User With Username "+inputMessageFromClient+" Not Found",K1));
                        clientOutputStream.flush();
                    }
                } else if (inputMessageFromClient.equals("<Exit>")){
                    clientOutputStream.println(Server.encryption.encryptMsg("you are Exited Successfully",K1));
                    clientOutputStream.flush();
                    client2=null;
                }
                else{
                    sendMessageToClient(inputMessageFromClient);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (Exception ignored) {

            }
        }


    }

    private void findClient(String inputMessageFromClient) {
        boolean flag = false;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).name.equals(inputMessageFromClient)) {
                client2 = clients.get(i);
                flag = true;
                break;
            }
        }
        if (!flag) {
            throw new RuntimeException("Client With username " + inputMessageFromClient + " Not Found");
        }
    }

    private void sendMessageToClient(String messageToReceiver) {
        if (messageToReceiver.isBlank())
            return;
        String K1=(Server.encryption instanceof RSA? public_key_string : key);
        try {
            // messageToReceiver = Client.aes.encrpytMsg(name + ": " + messageToReceiver, key, IV);
            messageToReceiver=Server.encryption.encryptMsg(name + ": " + messageToReceiver,K1);
        } catch (Exception e) {
            e.printStackTrace();
        }
        client2.clientOutputStream.println(messageToReceiver);
        client2.clientOutputStream.flush();
    }
    @Override
    public String toString() {
        return name;
    }
}