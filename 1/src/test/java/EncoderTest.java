import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncoderTest {

    @Test
    void testEncode() {
        Package pack = new Package((byte)1, 2, new Message(3,4, "test" ));
        Encoder encoder = new Encoder();
        byte[] encoded_bytes = encoder.encode(pack);

        String expected_Hex = "130100000000000000020000000c9769000000030000000474657374c8cb";

        assertEquals(expected_Hex, Hex.encodeHexString(encoded_bytes));
    }
}