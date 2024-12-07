import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client1 implements Runnable{
    private String clientName = null;
    private Socket socket = null;
    private Scanner inputStream = null;
    private PrintWriter outputStream = null;
    private BufferedReader response = null;

    public Client1(String hostName, int portNumber, String clientName) {

        this.clientName = clientName;

        while (true) { //retries for wrong port number

            try {
                socket = new Socket(hostName, portNumber);
                System.out.println(clientName + ", you're now connected!");
                System.out.println("Say Hello! to see your friends online..");
                inputStream = new Scanner(System.in);
                outputStream = new PrintWriter(socket.getOutputStream());
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
                outputStream.println(clientName+": "+line+"\n");
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
        System.out.print("Pls, enter your name here: ");
        Scanner in = new Scanner(System.in);

        while (true) {
            userName = in.nextLine();
            if (!userName.isBlank()) break;
            System.err.println("Name cannot be blank. Please enter your name");
        }

        String portNumber = "8888";
        System.out.println(portNumber);
        in.reset();
        int port = 8888;
        try {
            Thread cl = new Thread(new Client1("localhost", port, userName));
            cl.start();
        } catch (RuntimeException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }



    }
}