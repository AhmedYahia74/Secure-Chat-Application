import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;

public class Server {

    private final int portNumber;
    public static Encryption encryption;

    public Server(int portNUmber) throws IOException {
        this.portNumber = portNUmber;
        System.out.println("Choose an Encryption algorithm");
        System.out.println("1- RSA");
        System.out.println("2- AES");
        Scanner in =new Scanner(System.in);
        int n=in.nextInt();
        if(n==1){
            Server.encryption=new RSA();
        }
        else
            Server.encryption=new AES_Enryption();
        System.out.println(encryption);
    }

    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server started!");

            System.out.println("Waiting for a client to connect...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected." + LocalDateTime.now());
                System.out.println("---------------------");
                //Thread to handle client messages
                Thread client = new Thread(new CommunicationHandler(socket));
                client.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) throws  IOException {
        int port = 8888;
        System.out.println("You can now connect to this server via this port "+ port+"\n");
        Server server = new Server(port);
        server.startServer();
    }


}

