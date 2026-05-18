import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    @Test
    void testDecode() throws Exception {
        MyCipher.setTestKey();

        Package original = new Package((byte) 1, 2L, new Message(3, 4, "test"));

        Encoder encoder = new Encoder();
        byte[] encoded = encoder.encode(original);

        Decoder decoder = new Decoder();
        Package decoded = decoder.decode(encoded);

        assertEquals((byte) 1, decoded.getbSrc());
        assertEquals(2L, decoded.getbPktId());
        assertEquals(3, decoded.getMessage().getcType());
        assertEquals(4, decoded.getMessage().getbUserId());
        assertEquals("test", decoded.getMessage().getMessage());

        System.out.println("DecoderTest: Successful decode passed");
    }

    @Test
    void testDecodeInvalidHeaderCrc() {
        MyCipher.setTestKey();

        Package original = new Package((byte) 1, 2L, new Message(3, 4, "test"));
        Encoder encoder = new Encoder();
        byte[] data = encoder.encode(original);

        data[14] = (byte) (data[14] ^ 0xFF);

        Decoder decoder = new Decoder();

        assertThrows(RuntimeException.class,
                () -> decoder.decode(data),
                "Should throw exception on invalid header CRC");
    }

    @Test
    void testDecodeInvalidMessageCrc() {
        MyCipher.setTestKey();

        Package original = new Package((byte) 1, 2L, new Message(3, 4, "test"));
        Encoder encoder = new Encoder();
        byte[] data = encoder.encode(original);

        int lastIndex = data.length - 1;
        data[lastIndex] = (byte) (data[lastIndex] ^ 0xFF);

        Decoder decoder = new Decoder();

        assertThrows(RuntimeException.class,
                () -> decoder.decode(data),
                "Should throw exception on invalid message CRC");
    }
}