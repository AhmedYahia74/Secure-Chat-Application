import java.math.BigInteger;
import java.security.SecureRandom;

public class DiffieHellmanKeyExchange {
    // K= (A^b) mod p: secret key Calculated by Bob
    // B= (g^b) mod p
    // A= (g^a) mod p
    //where a is private key of Alice and b is private key of Bob
    // B: Bob's public key, A: Alice's public key , g: prime number, p: prime number

    public static BigInteger generatePrivateKey() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(256, random);
    }
    // A= (g^a) mod p: public key Calculated by Alice
    public static BigInteger generatePublicKey(BigInteger privateKey, BigInteger p, BigInteger g) {
        return g.modPow(privateKey, p);
    }
    // K= (A^b) mod p: secret key Calculated by Bob
    public static BigInteger generateSecretKey(BigInteger publicKey, BigInteger privateKey, BigInteger p) {
        return publicKey.modPow(privateKey, p);
    }

    public static void main(String[] args) {
        BigInteger p = new BigInteger("23");
        BigInteger g = new BigInteger("5");

        BigInteger a = generatePrivateKey();
        BigInteger A = generatePublicKey(a, p, g);

        BigInteger b = generatePrivateKey();
        BigInteger B = generatePublicKey(b, p, g);

        BigInteger secretKeyA = generateSecretKey(B, a, p);
        BigInteger secretKeyB = generateSecretKey(A, b, p);

        System.out.println("Alice's private key: " + a);
        System.out.println("Alice's public key: " + A);
        System.out.println("Bob's private key: " + b);
        System.out.println("Bob's public key: " + B);
        System.out.println("Secret key calculated by Alice: " + secretKeyA);
        System.out.println("Secret key calculated by Bob: " + secretKeyB);
    }



}
