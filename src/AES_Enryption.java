import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AES_Enryption {

    private int T_LEN = 128;
    private Cipher encryptionCipher;

    public static SecretKey GenerateSecretKey(int key_size) throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(key_size);
        SecretKey Key = generator.generateKey();
        return Key;
    }

    public String encrpytMsg(String message, SecretKey key) throws Exception {
        byte[] byteMsg = message.getBytes();
        encryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        encryptionCipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrpytedBytes = encryptionCipher.doFinal(byteMsg);
        System.out.println("Message is Sucessfully encrypted!...");
        String encodeEncryptedBytes = encode(encrpytedBytes);
        System.out.println("Encrypted Message: " + encodeEncryptedBytes);
        return encodeEncryptedBytes;
    }

    private String encode(byte[] data) {
        return Base64.getEncoder().encodeToString(data);
    }

    private byte[] decode(String data) {
        return Base64.getDecoder().decode(data);
    }

    public String decryptMsg(String encryptedMsg, SecretKey key) throws Exception {
        byte[] byteMsg = decode(encryptedMsg);
        Cipher decryptionCipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec spec = new GCMParameterSpec(T_LEN, encryptionCipher.getIV());
        decryptionCipher.init(Cipher.DECRYPT_MODE, key, spec);
        byte[] decryptedBytes = decryptionCipher.doFinal(byteMsg);
        String decryptedMessage = new String(decryptedBytes);
        System.out.println("The Message Decrypted Sucessfully!....");
        System.out.println("Decrypted Message: " + decryptedMessage);
        return decryptedMessage;

    }

   

}