import java.nio.ByteBuffer;

public class Encoder {

    public byte[] encode(Package pack) {
        Message message = pack.getMessage();
        byte[] messageBytes = message.getMessage().getBytes();
        int messageDataLength = 4 + 4 + messageBytes.length;

        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + 2 + messageDataLength + 2);

        buffer.put((byte) 0x13);
        buffer.put(pack.getbSrc());
        buffer.putLong(pack.getbPktId());
        buffer.putInt(messageDataLength);

        buffer.putShort(Crc16.calculateCrc(buffer.array(), 0, buffer.position()));

        int messageStart = buffer.position();
        buffer.putInt(message.getcType());
        buffer.putInt(message.getbUserId());
        buffer.put(messageBytes);

        buffer.putShort(Crc16.calculateCrc(buffer.array(), messageStart, messageDataLength));

        return buffer.array();
    }
}
