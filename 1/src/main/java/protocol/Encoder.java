import java.nio.ByteBuffer;

public class Encoder {

    public byte[] encode(Package pack) {
        Message message = pack.getMessage();
        byte[] encryptedMessage;
        try {
            encryptedMessage = MyCipher.encrypt(message.getMessage());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
        int messageDataLength = 4 + 4 + encryptedMessage.length;

        ByteBuffer buffer = ByteBuffer.allocate(1 + 1 + 8 + 4 + 2 + messageDataLength + 2);

        buffer.put((byte) 0x13);
        buffer.put(pack.getbSrc());
        buffer.putLong(pack.getbPktId());
        buffer.putInt(messageDataLength);

        buffer.putShort(Crc16.calculateCrc(buffer.array(), 0, buffer.position()));

        int messageStart = buffer.position();
        buffer.putInt(message.getcType());
        buffer.putInt(message.getbUserId());
        buffer.put(encryptedMessage);

        buffer.putShort(Crc16.calculateCrc(buffer.array(), messageStart, messageDataLength));

        return buffer.array();
    }
}
