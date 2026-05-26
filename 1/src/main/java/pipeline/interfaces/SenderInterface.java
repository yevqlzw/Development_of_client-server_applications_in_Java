package pipeline.interfaces;

import java.net.InetAddress;

public interface SenderInterface {
    void sendMessage(byte[] mess, InetAddress target);
}