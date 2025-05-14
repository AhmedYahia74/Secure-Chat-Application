import javax.crypto.SecretKey;

public interface Encryption {
    String encryptMsg(String message, String Key)throws Exception;
    String decryptMsg(String message,String Key) throws Exception;
}
