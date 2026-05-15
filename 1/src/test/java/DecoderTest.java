import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DecoderTest {
    @Test
    void testDecode() throws DecoderException {

        Decoder decoder = new Decoder();
        String encodedHex = "130100000000000000020000000c9769000000030000000474657374c8cb";

        byte [] expectedBytes = Hex.decodeHex(encodedHex);

        Package pack = decoder.decode(expectedBytes);

        assertEquals(pack.getbSrc(), (byte)1);
        assertEquals(pack.getbPktId(), 2);
        assertEquals(pack.getMessage().getcType(), 3);
        assertEquals(pack.getMessage().getbUserId(), 4);
        assertEquals(pack.getMessage().getMessage(), "test");
    }
}