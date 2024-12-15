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
    private String key = "rnLgcmZmVZDsTreCCiiryA==";
    private String IV = "jQFIcwdbvVMRjjxk";
    AES_Enryption aes = new AES_Enryption();
    RSA rsa = new RSA();
    private String public_key_string = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlle1xN4CCjwQZcuWKGDl+eUkdx2Fwab411b5wKP8NlXCr7pyHpGAdFpUh+X4FLy2+ugO+atyMoDAuAVMdoh7y7NuGPpY9v3SlCN4IZII4PuxrRhplu6fDYa5UnOun7OO5CIgjZglp7s7BsoNvnAgUzCmDH9JQCLJSB849RFKbgXw6v1dfY3jic21pcfHicYjH/kDPE5Adok7nHEsE88HkgJotHlvCCRw8pqsWF3AldaePrU3FweSXds1/j4C/czOaI/U45RD0oF0YdeK+WzoDFQsLTIn7w5oIRDRtmbsRmjcjjajFFv54EFLv7AKICbsv1jTgB7kwPTtTFqIUjuV+QIDAQAB";
    private String private_key_string = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCWV7XE3gIKPBBly5YoYOX55SR3HYXBpvjXVvnAo/w2VcKvunIekYB0WlSH5fgUvLb66A75q3IygMC4BUx2iHvLs24Y+lj2/dKUI3ghkgjg+7GtGGmW7p8NhrlSc66fs47kIiCNmCWnuzsGyg2+cCBTMKYMf0lAIslIHzj1EUpuBfDq/V19jeOJzbWlx8eJxiMf+QM8TkB2iTuccSwTzweSAmi0eW8IJHDymqxYXcCV1p4+tTcXB5Jd2zX+PgL9zM5oj9TjlEPSgXRh14r5bOgMVCwtMifvDmghENG2ZuxGaNyONqMUW/ngQUu/sAogJuy/WNOAHuTA9O1MWohSO5X5AgMBAAECggEADE/DdWCJgeVi9wsXJq2vYj73ClVzFNAudD1NojkWQtbUD3LxEdAa2QG6sWROOViDktl6ZyIN7dUoXbQlYQGSmF7/EZw9Y9XrrtuWuge3osD0ebcpknAZEKZV5SAkY99JliS1+L0bEaNzVSjVEPbQAYVW2714C1YDQ1zh265zo2hzV+mD4VfCz69tFZywk3uX/+oWNpCoqFdAff80AN6tqlTC2fmNb1iz4srQTZWVSlvZk91LnTVk3aq6qb9BEWRoINCTU6DtBxP7JEAInvMojNthaGbXlIBylRvvqzrFpfe2+gVPk6dKjS+JLaTOOxrUd9XUflQY0i1TfwN7yJSY7wKBgQDC+WYSWIqjYxeZyu/94nyen3knIh6Bz8u5TIgzMJWanFAnh6FHpKU0UIYRWGCBtBg+Z7K1puXmXIHQd3GcBErURiABqXHnzKt5oGQw5qGyzphs3z3B69NNn93FSyIh2DXzFPf52D1yFMo6oZJT554q7xCNfLWGxEqGmBwhSn95nwKBgQDFZiNBoI7Sj+lDNruKRG+ZZvvLkR4b257Wc6Lias/a5I0wzXzoRyYCmRBEpK4j+T7Ldy+PRVwb+Jd2gEaADNPcnQJWiVg0q3eR6kQME3zugDD3XuyRi/JhWlrA9wqmWX7YgUCvBjE8MNcUDPPgaHxwX4tM+lwvztGMB4F4jpr5ZwKBgQCoqoNoX3wfd7uU6X/PS7yupBp0hgmKFq6QL+qrDd59j7evWp9kkMPxi69PFfr2eUt3wNFSX30GWQRbyNhZNUVeeQN7LJBDDEVSxDOoMfuz6RDnLgAI3+89eYyp/iMa0CVrkborQqt1IxMGwXsKZpXnYkQZgcavPOOTp8a97ep01QKBgFLFORNTl4+C+HROhuS7PXA9VmdNOirENB4H7syxrOZD31APWcirzKxaMhAWXU6IPGRkXXTdyHmSCzCNKQKYXl2rGEfg3zN2knSEnnPR2BjJd77B9sAwxjk8AcHX1IdcD2wJBm5dUlfCwuyNYdU++q7D4U0tzWneds8Ydplucl0RAoGAZaPVSUShftN97K9lVvSWKLcd/MjeyhObqzGKC10PMhHjhqpQ54YU39tZgBnBrtN4Kyn8zrIwr/SVnzdQu4vgboAxkyI3RMFhkCGaS5WimufJUYJOkSDxXgoqkEFoZkb9TzqzqsvhxWdgZDO399PxnqKyUiFHBPM7e5EDj+Uk8v0=";


    public CommunicationHandler(Socket clientSocket) {
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