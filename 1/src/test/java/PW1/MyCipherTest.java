package PW1;

import protocol.MyCipher;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyCipherTest {

    @Test
    void testEncryption () throws Exception {
        String originalText = "It's a secret message!";
        MyCipher.setTestKey();
        byte[] encryptedText = MyCipher.encrypt(originalText);

        assertNotNull(encryptedText, "Encrypted text should not be null");
        assertNotEquals(originalText, new String(encryptedText), "Encrypted text should not be the same");
        System.out.println("Encryption complete successfully. Length: " + encryptedText.length);
        System.out.println("Encrypted text (hex): " + Hex.encodeHexString(encryptedText));
    }


    @Test
    void testDecryption () throws Exception {
        String originalText = "It's a secret message!";

        MyCipher.setTestKey();
        byte[] encryptedData = MyCipher.encrypt(originalText);
        String decryptedText = MyCipher.decrypt(encryptedData);

        assertEquals(originalText, decryptedText, "Decrypted text is not the same");
        System.out.println("Decryption complete successfully. Length: " + encryptedData.length);
    }
}