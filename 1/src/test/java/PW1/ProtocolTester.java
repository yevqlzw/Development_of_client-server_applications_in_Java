package PW1;

import protocol.Decoder;
import protocol.Encoder;
import protocol.Message;
import protocol.MyCipher;
import protocol.Package;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProtocolTester {

    @Test
    void testFullProtocol() throws Exception {
        MyCipher.setTestKey();

        Package original = new protocol.Package((byte) 1, 2L, new Message(3, 4, "It's a secret message!"));

        byte[] encoded = new Encoder().encode(original);
        System.out.println("Encoded (hex): " + bytesToHex(encoded));

        protocol.Package decoded = new Decoder().decode(encoded);
        System.out.println("Protocol.Message: " + decoded.getMessage().getMessage());

        assertEquals(original, decoded);
        System.out.println("All assertions passed! Protocol works correctly.");
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }
}