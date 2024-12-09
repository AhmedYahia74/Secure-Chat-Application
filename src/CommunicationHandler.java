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

    public CommunicationHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
        try {
            this.clientInputStream = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.clientOutputStream = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
            this.name=clientInputStream.readLine();
            clients.add(this);
            System.out.println(clients);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }


    @Override
    public void run() {

        String inputMessageFromClient = "", recipientName = "", messageToReceiver = "";
        while (true) {
            try {
                if (!clientInputStream.ready())
                    continue;
                inputMessageFromClient = clientInputStream.readLine();
                if (client2 == null) {
                    try {
                        findClient(inputMessageFromClient);
                        clientOutputStream.println("Connected Successfully With " + client2.name);
                        clientOutputStream.flush();
                    } catch (Exception ex) {
                        clientOutputStream.println(ex.getMessage());
                        clientInputStream.readLine();
                        clientOutputStream.flush();
                    }
                } else if (inputMessageFromClient.equals("<Exit>")){
                    clientInputStream.readLine();
                    clientOutputStream.println("you are Exited Successfully");
                    clientOutputStream.flush();
                    client2=null;
                }
                else{
                    sendMessageToClient(inputMessageFromClient);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            } catch (NoSuchElementException ignored) {

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
            throw new RuntimeException("Client With username " + inputMessageFromClient + " Not Found!");
        }
    }

    private void sendMessageToClient(String messageToReceiver) {
        if (messageToReceiver.isBlank())
            return;

        client2.clientOutputStream.println(name+": "+messageToReceiver);
        client2.clientOutputStream.flush();
    }
    @Override
    public String toString() {
        return name;
    }
}