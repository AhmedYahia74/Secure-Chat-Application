import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;


public class CommunicationHandler implements Runnable {

    private String name = null;
    public BufferedReader clientInputStream1 = null, clientInputStream2=null;
    public PrintWriter clientOutputStream1 = null,clientOutputStream2 = null;
    private Socket clientSocket1,clientSocket2;

    public CommunicationHandler(Socket clientSocket1, Socket clientSocket2) {
        this.name = "Client";
        this.clientSocket1 = clientSocket1;
        this.clientSocket2 = clientSocket2;
        try {
            this.clientInputStream1 = new BufferedReader(new InputStreamReader(clientSocket1.getInputStream()));
            this.clientOutputStream1 = new PrintWriter(new OutputStreamWriter(clientSocket1.getOutputStream()));
            this.clientInputStream2 = new BufferedReader(new InputStreamReader(clientSocket2.getInputStream()));
            this.clientOutputStream2 = new PrintWriter(new OutputStreamWriter(clientSocket2.getOutputStream()));
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void run() {

        String inputMessageFromClient = "", recipientName = "", messageToReceiver = "";

        while (true) {
            try {

                if(clientInputStream1.ready()){
                    inputMessageFromClient = clientInputStream1.readLine();
                    sendMessageToClient2(inputMessageFromClient);
                    clientInputStream1.readLine();
                }

                if(clientInputStream2.ready()){
                    inputMessageFromClient = clientInputStream2.readLine();
                    sendMessageToClient1(inputMessageFromClient);
                    clientInputStream2.readLine();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchElementException ignored) {

            }
        }

    }

    private void sendMessageToClient1(String messageToReceiver) {
        clientOutputStream1.println(messageToReceiver);
        clientOutputStream1.flush();

        clientOutputStream2.println(">> Your message was sent successfully!");
        clientOutputStream2.flush();
    }

    private void sendMessageToClient2(String messageToReceiver) {
        clientOutputStream2.println(messageToReceiver);
        clientOutputStream2.flush();

        clientOutputStream1.println(">> Your message was sent successfully!");
        clientOutputStream1.flush();
    }


    @Override
    public String toString() {
        return name;
    }
}