package PW1;

import protocol.Decoder;
import protocol.Encoder;
import protocol.Message;
import protocol.MyCipher;
import protocol.Package;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {

    @Test
    void testDecode() throws Exception {
        MyCipher.setTestKey();

        protocol.Package original = new protocol.Package((byte) 1, 2L, new Message(3, 4, "test"));

        Encoder encoder = new Encoder();
        byte[] encoded = encoder.encode(original);

        Decoder decoder = new Decoder();
        protocol.Package decoded = decoder.decode(encoded);

        assertEquals((byte) 1, decoded.getbSrc());
        assertEquals(2L, decoded.getbPktId());
        assertEquals(3, decoded.getMessage().getcType());
        assertEquals(4, decoded.getMessage().getbUserId());
        assertEquals("test", decoded.getMessage().getMessage());

        System.out.println("PW1.DecoderTest: Successful decode passed");
    }

    @Test
    void testDecodeInvalidHeaderCrc() {
        MyCipher.setTestKey();

        protocol.Package original = new protocol.Package((byte) 1, 2L, new Message(3, 4, "test"));
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

        Package original = new protocol.Package((byte) 1, 2L, new Message(3, 4, "test"));
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