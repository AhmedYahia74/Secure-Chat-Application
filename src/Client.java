import javax.crypto.SecretKey;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;
public class Client implements Runnable{
    private String clientName = null;
    private Socket socket = null;
    private Scanner inputStream = null;
    private PrintWriter outputStream = null;
    private BufferedReader response = null;
    public static AES_Enryption aes = new AES_Enryption();
    public static RSA rsa = new RSA();
    private String key = "rnLgcmZmVZDsTreCCiiryA==";
    private String IV = "jQFIcwdbvVMRjjxk";
    private String public_key_string = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlle1xN4CCjwQZcuWKGDl+eUkdx2Fwab411b5wKP8NlXCr7pyHpGAdFpUh+X4FLy2+ugO+atyMoDAuAVMdoh7y7NuGPpY9v3SlCN4IZII4PuxrRhplu6fDYa5UnOun7OO5CIgjZglp7s7BsoNvnAgUzCmDH9JQCLJSB849RFKbgXw6v1dfY3jic21pcfHicYjH/kDPE5Adok7nHEsE88HkgJotHlvCCRw8pqsWF3AldaePrU3FweSXds1/j4C/czOaI/U45RD0oF0YdeK+WzoDFQsLTIn7w5oIRDRtmbsRmjcjjajFFv54EFLv7AKICbsv1jTgB7kwPTtTFqIUjuV+QIDAQAB";
    private String private_key_string = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCWV7XE3gIKPBBly5YoYOX55SR3HYXBpvjXVvnAo/w2VcKvunIekYB0WlSH5fgUvLb66A75q3IygMC4BUx2iHvLs24Y+lj2/dKUI3ghkgjg+7GtGGmW7p8NhrlSc66fs47kIiCNmCWnuzsGyg2+cCBTMKYMf0lAIslIHzj1EUpuBfDq/V19jeOJzbWlx8eJxiMf+QM8TkB2iTuccSwTzweSAmi0eW8IJHDymqxYXcCV1p4+tTcXB5Jd2zX+PgL9zM5oj9TjlEPSgXRh14r5bOgMVCwtMifvDmghENG2ZuxGaNyONqMUW/ngQUu/sAogJuy/WNOAHuTA9O1MWohSO5X5AgMBAAECggEADE/DdWCJgeVi9wsXJq2vYj73ClVzFNAudD1NojkWQtbUD3LxEdAa2QG6sWROOViDktl6ZyIN7dUoXbQlYQGSmF7/EZw9Y9XrrtuWuge3osD0ebcpknAZEKZV5SAkY99JliS1+L0bEaNzVSjVEPbQAYVW2714C1YDQ1zh265zo2hzV+mD4VfCz69tFZywk3uX/+oWNpCoqFdAff80AN6tqlTC2fmNb1iz4srQTZWVSlvZk91LnTVk3aq6qb9BEWRoINCTU6DtBxP7JEAInvMojNthaGbXlIBylRvvqzrFpfe2+gVPk6dKjS+JLaTOOxrUd9XUflQY0i1TfwN7yJSY7wKBgQDC+WYSWIqjYxeZyu/94nyen3knIh6Bz8u5TIgzMJWanFAnh6FHpKU0UIYRWGCBtBg+Z7K1puXmXIHQd3GcBErURiABqXHnzKt5oGQw5qGyzphs3z3B69NNn93FSyIh2DXzFPf52D1yFMo6oZJT554q7xCNfLWGxEqGmBwhSn95nwKBgQDFZiNBoI7Sj+lDNruKRG+ZZvvLkR4b257Wc6Lias/a5I0wzXzoRyYCmRBEpK4j+T7Ldy+PRVwb+Jd2gEaADNPcnQJWiVg0q3eR6kQME3zugDD3XuyRi/JhWlrA9wqmWX7YgUCvBjE8MNcUDPPgaHxwX4tM+lwvztGMB4F4jpr5ZwKBgQCoqoNoX3wfd7uU6X/PS7yupBp0hgmKFq6QL+qrDd59j7evWp9kkMPxi69PFfr2eUt3wNFSX30GWQRbyNhZNUVeeQN7LJBDDEVSxDOoMfuz6RDnLgAI3+89eYyp/iMa0CVrkborQqt1IxMGwXsKZpXnYkQZgcavPOOTp8a97ep01QKBgFLFORNTl4+C+HROhuS7PXA9VmdNOirENB4H7syxrOZD31APWcirzKxaMhAWXU6IPGRkXXTdyHmSCzCNKQKYXl2rGEfg3zN2knSEnnPR2BjJd77B9sAwxjk8AcHX1IdcD2wJBm5dUlfCwuyNYdU++q7D4U0tzWneds8Ydplucl0RAoGAZaPVSUShftN97K9lVvSWKLcd/MjeyhObqzGKC10PMhHjhqpQ54YU39tZgBnBrtN4Kyn8zrIwr/SVnzdQu4vgboAxkyI3RMFhkCGaS5WimufJUYJOkSDxXgoqkEFoZkb9TzqzqsvhxWdgZDO399PxnqKyUiFHBPM7e5EDj+Uk8v0=";

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
                line = inputStream.nextLine();
                if(line==null)
                    continue;
                try {
                    if (!line.isBlank())
                        // line = aes.encrpytMsg(line, key, IV);
                        line = rsa.encryptMsg(line, public_key_string);
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
                try {
                    line = response.readLine();
                    if (line == null)
                        break;
                    if (!line.isBlank())
                        //line = aes.decryptMsg(line, key, IV);
                       line = rsa.decryptMsg(line, private_key_string);
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