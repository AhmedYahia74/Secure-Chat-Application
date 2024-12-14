import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

public class RSA {
    private PrivateKey privateKey;
    private PublicKey publicKey;
    //for testing
    private String public_key_string = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlle1xN4CCjwQZcuWKGDl+eUkdx2Fwab411b5wKP8NlXCr7pyHpGAdFpUh+X4FLy2+ugO+atyMoDAuAVMdoh7y7NuGPpY9v3SlCN4IZII4PuxrRhplu6fDYa5UnOun7OO5CIgjZglp7s7BsoNvnAgUzCmDH9JQCLJSB849RFKbgXw6v1dfY3jic21pcfHicYjH/kDPE5Adok7nHEsE88HkgJotHlvCCRw8pqsWF3AldaePrU3FweSXds1/j4C/czOaI/U45RD0oF0YdeK+WzoDFQsLTIn7w5oIRDRtmbsRmjcjjajFFv54EFLv7AKICbsv1jTgB7kwPTtTFqIUjuV+QIDAQAB";
    private String private_key_string = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCWV7XE3gIKPBBly5YoYOX55SR3HYXBpvjXVvnAo/w2VcKvunIekYB0WlSH5fgUvLb66A75q3IygMC4BUx2iHvLs24Y+lj2/dKUI3ghkgjg+7GtGGmW7p8NhrlSc66fs47kIiCNmCWnuzsGyg2+cCBTMKYMf0lAIslIHzj1EUpuBfDq/V19jeOJzbWlx8eJxiMf+QM8TkB2iTuccSwTzweSAmi0eW8IJHDymqxYXcCV1p4+tTcXB5Jd2zX+PgL9zM5oj9TjlEPSgXRh14r5bOgMVCwtMifvDmghENG2ZuxGaNyONqMUW/ngQUu/sAogJuy/WNOAHuTA9O1MWohSO5X5AgMBAAECggEADE/DdWCJgeVi9wsXJq2vYj73ClVzFNAudD1NojkWQtbUD3LxEdAa2QG6sWROOViDktl6ZyIN7dUoXbQlYQGSmF7/EZw9Y9XrrtuWuge3osD0ebcpknAZEKZV5SAkY99JliS1+L0bEaNzVSjVEPbQAYVW2714C1YDQ1zh265zo2hzV+mD4VfCz69tFZywk3uX/+oWNpCoqFdAff80AN6tqlTC2fmNb1iz4srQTZWVSlvZk91LnTVk3aq6qb9BEWRoINCTU6DtBxP7JEAInvMojNthaGbXlIBylRvvqzrFpfe2+gVPk6dKjS+JLaTOOxrUd9XUflQY0i1TfwN7yJSY7wKBgQDC+WYSWIqjYxeZyu/94nyen3knIh6Bz8u5TIgzMJWanFAnh6FHpKU0UIYRWGCBtBg+Z7K1puXmXIHQd3GcBErURiABqXHnzKt5oGQw5qGyzphs3z3B69NNn93FSyIh2DXzFPf52D1yFMo6oZJT554q7xCNfLWGxEqGmBwhSn95nwKBgQDFZiNBoI7Sj+lDNruKRG+ZZvvLkR4b257Wc6Lias/a5I0wzXzoRyYCmRBEpK4j+T7Ldy+PRVwb+Jd2gEaADNPcnQJWiVg0q3eR6kQME3zugDD3XuyRi/JhWlrA9wqmWX7YgUCvBjE8MNcUDPPgaHxwX4tM+lwvztGMB4F4jpr5ZwKBgQCoqoNoX3wfd7uU6X/PS7yupBp0hgmKFq6QL+qrDd59j7evWp9kkMPxi69PFfr2eUt3wNFSX30GWQRbyNhZNUVeeQN7LJBDDEVSxDOoMfuz6RDnLgAI3+89eYyp/iMa0CVrkborQqt1IxMGwXsKZpXnYkQZgcavPOOTp8a97ep01QKBgFLFORNTl4+C+HROhuS7PXA9VmdNOirENB4H7syxrOZD31APWcirzKxaMhAWXU6IPGRkXXTdyHmSCzCNKQKYXl2rGEfg3zN2knSEnnPR2BjJd77B9sAwxjk8AcHX1IdcD2wJBm5dUlfCwuyNYdU++q7D4U0tzWneds8Ydplucl0RAoGAZaPVSUShftN97K9lVvSWKLcd/MjeyhObqzGKC10PMhHjhqpQ54YU39tZgBnBrtN4Kyn8zrIwr/SVnzdQu4vgboAxkyI3RMFhkCGaS5WimufJUYJOkSDxXgoqkEFoZkb9TzqzqsvhxWdgZDO399PxnqKyUiFHBPM7e5EDj+Uk8v0=";

    public static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return keyPair;
    }
    
    public void get_public_key(String public_key_string) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decode(public_key_string));
            publicKey = keyFactory.generatePublic(publicKeySpec);
        } catch (Exception e) {
            // TODO: handle exception
            
        }

    }

    public void get_private_key(String private_key_string) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decode(private_key_string));
            privateKey = keyFactory.generatePrivate(privateKeySpec);
        } catch (Exception e) {
            
        }
    }

    public String encryptMsg(String message, String publicKey_string) throws Exception {
        get_public_key(publicKey_string);
        byte[] byteMsg = message.getBytes();
        Cipher encryptionCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        encryptionCipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encrpytedBytes = encryptionCipher.doFinal(byteMsg);
        // System.out.println("Message is Sucessfully encrypted!...");
        String encodeEncryptedBytes = encode(encrpytedBytes);
        // System.out.println("Encrypted Message: " + encodeEncryptedBytes);
        return encodeEncryptedBytes;
    }

    private String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    public String decryptMsg(String encryptedMsg, String privateKey_string) throws Exception {
        get_private_key(privateKey_string);
        byte[] byteMsg = decode(encryptedMsg);
        Cipher decryptionCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        decryptionCipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decryptedBytes = decryptionCipher.doFinal(byteMsg);
        // System.out.println("Message is Sucessfully decrypted!...");
        String decryptedMessage = new String(decryptedBytes, "UTF-8");
        // System.out.println("Decrypted Message: " + decryptedMessage);
        return decryptedMessage;
    }

    private byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public void printKeys() {
        System.out.println("Public Key: " + encode(publicKey.getEncoded()));
        System.out.println("Private Key: " + encode(privateKey.getEncoded()));
    }
    // public static void main(String[] args) {
    //     RSA rsa = new RSA();
    //     String public_key_string = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAlle1xN4CCjwQZcuWKGDl+eUkdx2Fwab411b5wKP8NlXCr7pyHpGAdFpUh+X4FLy2+ugO+atyMoDAuAVMdoh7y7NuGPpY9v3SlCN4IZII4PuxrRhplu6fDYa5UnOun7OO5CIgjZglp7s7BsoNvnAgUzCmDH9JQCLJSB849RFKbgXw6v1dfY3jic21pcfHicYjH/kDPE5Adok7nHEsE88HkgJotHlvCCRw8pqsWF3AldaePrU3FweSXds1/j4C/czOaI/U45RD0oF0YdeK+WzoDFQsLTIn7w5oIRDRtmbsRmjcjjajFFv54EFLv7AKICbsv1jTgB7kwPTtTFqIUjuV+QIDAQAB";
    //     String private_key_string = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCWV7XE3gIKPBBly5YoYOX55SR3HYXBpvjXVvnAo/w2VcKvunIekYB0WlSH5fgUvLb66A75q3IygMC4BUx2iHvLs24Y+lj2/dKUI3ghkgjg+7GtGGmW7p8NhrlSc66fs47kIiCNmCWnuzsGyg2+cCBTMKYMf0lAIslIHzj1EUpuBfDq/V19jeOJzbWlx8eJxiMf+QM8TkB2iTuccSwTzweSAmi0eW8IJHDymqxYXcCV1p4+tTcXB5Jd2zX+PgL9zM5oj9TjlEPSgXRh14r5bOgMVCwtMifvDmghENG2ZuxGaNyONqMUW/ngQUu/sAogJuy/WNOAHuTA9O1MWohSO5X5AgMBAAECggEADE/DdWCJgeVi9wsXJq2vYj73ClVzFNAudD1NojkWQtbUD3LxEdAa2QG6sWROOViDktl6ZyIN7dUoXbQlYQGSmF7/EZw9Y9XrrtuWuge3osD0ebcpknAZEKZV5SAkY99JliS1+L0bEaNzVSjVEPbQAYVW2714C1YDQ1zh265zo2hzV+mD4VfCz69tFZywk3uX/+oWNpCoqFdAff80AN6tqlTC2fmNb1iz4srQTZWVSlvZk91LnTVk3aq6qb9BEWRoINCTU6DtBxP7JEAInvMojNthaGbXlIBylRvvqzrFpfe2+gVPk6dKjS+JLaTOOxrUd9XUflQY0i1TfwN7yJSY7wKBgQDC+WYSWIqjYxeZyu/94nyen3knIh6Bz8u5TIgzMJWanFAnh6FHpKU0UIYRWGCBtBg+Z7K1puXmXIHQd3GcBErURiABqXHnzKt5oGQw5qGyzphs3z3B69NNn93FSyIh2DXzFPf52D1yFMo6oZJT554q7xCNfLWGxEqGmBwhSn95nwKBgQDFZiNBoI7Sj+lDNruKRG+ZZvvLkR4b257Wc6Lias/a5I0wzXzoRyYCmRBEpK4j+T7Ldy+PRVwb+Jd2gEaADNPcnQJWiVg0q3eR6kQME3zugDD3XuyRi/JhWlrA9wqmWX7YgUCvBjE8MNcUDPPgaHxwX4tM+lwvztGMB4F4jpr5ZwKBgQCoqoNoX3wfd7uU6X/PS7yupBp0hgmKFq6QL+qrDd59j7evWp9kkMPxi69PFfr2eUt3wNFSX30GWQRbyNhZNUVeeQN7LJBDDEVSxDOoMfuz6RDnLgAI3+89eYyp/iMa0CVrkborQqt1IxMGwXsKZpXnYkQZgcavPOOTp8a97ep01QKBgFLFORNTl4+C+HROhuS7PXA9VmdNOirENB4H7syxrOZD31APWcirzKxaMhAWXU6IPGRkXXTdyHmSCzCNKQKYXl2rGEfg3zN2knSEnnPR2BjJd77B9sAwxjk8AcHX1IdcD2wJBm5dUlfCwuyNYdU++q7D4U0tzWneds8Ydplucl0RAoGAZaPVSUShftN97K9lVvSWKLcd/MjeyhObqzGKC10PMhHjhqpQ54YU39tZgBnBrtN4Kyn8zrIwr/SVnzdQu4vgboAxkyI3RMFhkCGaS5WimufJUYJOkSDxXgoqkEFoZkb9TzqzqsvhxWdgZDO399PxnqKyUiFHBPM7e5EDj+Uk8v0=";

    //     try {
    //         String encryptedMsg = rsa.encryptMsg("   Hello World! ", public_key_string);
    //         rsa.decryptMsg(encryptedMsg, private_key_string);
            
    //         //rsa.printKeys();
    //     } catch (Exception e) {
    //         System.out.println(e);
    //     }
    // }
}
