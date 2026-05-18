import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncoderTest {

    @Test
    void testEncode() {
        MyCipher.setTestKey();

        Package pack = new Package((byte) 1, 2, new Message(3, 4, "test"));
        Encoder encoder = new Encoder();
        byte[] encoded = encoder.encode(pack);

        assertNotNull(encoded);

        assertEquals((byte) 0x13, encoded[0]);
        assertEquals((byte) 1, encoded[1]);
        assertEquals(0, encoded[2]);
        assertEquals(0, encoded[3]);
        assertEquals(0, encoded[4]);
        assertEquals(0, encoded[5]);
        assertEquals(0, encoded[6]);
        assertEquals(0, encoded[7]);

        System.out.println("EncoderTest passed. Package length: " + encoded.length);
    }
}