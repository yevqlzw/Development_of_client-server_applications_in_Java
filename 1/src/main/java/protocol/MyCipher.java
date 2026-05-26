package protocol;

import javax.crypto.*;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

public class MyCipher {
    private static SecretKey secretKey;
    private static final String transformation = "AES/ECB/PKCS5Padding";

    private static final byte[] TEST_KEY_BYTES = new byte[16];

    static {
        try {
            secretKey = generateKey(128); // AES-128
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Cannot initialize AES key", e);
        }
    }

    public static SecretKey generateKey(int keyLength)  throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(keyLength);
        return keyGenerator.generateKey();
    }

    public static void setTestKey() {
        try {
            secretKey = new javax.crypto.spec.SecretKeySpec(TEST_KEY_BYTES, "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static void setKey(SecretKey newKey) {
        if (!"AES".equals(newKey.getAlgorithm())) {
            throw new IllegalArgumentException("Key should be for AES. Unsupported: " + newKey.getAlgorithm());        }
        secretKey = newKey;
    }


    public static SecretKey getKey() {
        return secretKey;
    }


    public static byte[] encrypt(String text) throws Exception {
        if (text == null) { return new byte[0]; }
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
    }


        public static String decrypt(byte[] encryptedBytes) throws Exception {
        if (encryptedBytes == null||encryptedBytes.length==0) { return ""; }
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(encryptedBytes), StandardCharsets.UTF_8);
        }

}
