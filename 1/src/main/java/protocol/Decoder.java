package protocol;

import java.nio.ByteBuffer;

public class Decoder {
    public Package decode (byte[] data) {
       Package pack = new Package();
            ByteBuffer bytes = ByteBuffer.wrap(data);
            byte magic = bytes.get();
            if (magic != 0x13) {
            throw new RuntimeException("Invalid magic byte: " + magic);
            }
            pack.setbSrc(bytes.get());
            pack.setbPktId(bytes.getLong());

            int messageLength = bytes.getInt();

            short header_crc16 = Crc16.calculateCrc(data, 0, 1+1+8+4);
            if(header_crc16 != bytes.getShort()) {
                throw new RuntimeException("Invalid Header CRC16!");
            }

            int cType = bytes.getInt();
            int bUserId = bytes.getInt();

            int encryptedTextLength = messageLength-4-4;
            byte[] encryptedText = new byte[encryptedTextLength];
            bytes.get(encryptedText);

            String decryptedText;
        try {
            decryptedText = MyCipher.decrypt(encryptedText);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }

        pack.setMessage(new Message(cType, bUserId, decryptedText));


        short message_crc16 = Crc16.calculateCrc(data, 16, messageLength);
        if (message_crc16 != bytes.getShort()) {
            throw new RuntimeException("Invalid Protocol.Message CRC16!");
        }
        return pack;
    }
}
