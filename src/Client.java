import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client implements Runnable{
    private String clientName = null;
    private Socket socket = null;
    private Scanner inputStream = null;
    private PrintWriter outputStream = null;
    private BufferedReader response = null;

    public Client(String hostName, int portNumber, String clientName) {

        this.clientName = clientName;

        while (true) {
            try {
                socket = new Socket(hostName, portNumber);
                System.out.println("you're now connected!");
                inputStream = new Scanner(System.in);
                outputStream = new PrintWriter(socket.getOutputStream());
                response=new BufferedReader(new InputStreamReader(socket.getInputStream()));
                outputStream.println(clientName);
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
                line = inputStream.nextLine();
                outputStream.println(line+"\n");
                outputStream.flush(); //Prevents buffering of the message. Sends out the message immediately
            }
        });
    }

    public Thread getReceiver() {
        return new Thread(() -> {
            String line = "";
            while (true) {
                try {
                    line = response.readLine();
                    if (line == null)
                        break;
                    System.out.println(line);
                } catch (IOException e) {
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
                System.out.println("Name Cannot be blank!");
                continue;
            }

            if (findClient(userName)) {
                System.out.println("Name Found already!");
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
        boolean flag = false;
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