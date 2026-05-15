import java.nio.ByteBuffer;

public class Decoder {
    public Package decode (byte[] data) {
       Package pack = new Package();
            ByteBuffer bytes = ByteBuffer.wrap(data);
            bytes.get();

            pack.setbSrc(bytes.get());
            pack.setbPktId(bytes.getLong());

            int messageLength = bytes.getInt();

            short header_crc16 = Crc16.calculateCrc(data, 0, 1+1+8+4);
            if(header_crc16 != bytes.getShort()) {
                throw new RuntimeException("Invalid Header CRC16!");
            }

            int cType = bytes.getInt();
            int bUserId = bytes.getInt();
            int textLength = messageLength-4-4;

            pack.setMessage(new Message(cType, bUserId, new String(bytes.array(), 24, textLength)));
            bytes.position(24+textLength);


            short message_crc16 = Crc16.calculateCrc(data, 16, messageLength);
            if (message_crc16 != bytes.getShort()) {
                throw new RuntimeException("Invalid Message CRC16!");
            }
            return pack;
    }
}
