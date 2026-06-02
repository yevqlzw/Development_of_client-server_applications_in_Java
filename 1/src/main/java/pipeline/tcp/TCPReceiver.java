package pipeline.tcp;

import pipeline.ClientBytes;
import java.io.DataInputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPReceiver implements Runnable {
    private final Socket socket;
    private final BlockingQueue<ClientBytes> queue;
    private final AtomicBoolean running;

    public TCPReceiver(Socket s, BlockingQueue<ClientBytes> q, AtomicBoolean r) {
        socket = s; queue = q; running = r;
    }

    @Override
    public void run() {
        try (DataInputStream dis = new DataInputStream(socket.getInputStream())) {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                int len = dis.readInt();
                if (len <= 0 || len > 65536) { //64 KB
                    System.err.println("Invalid packet length: " + len + ", closing connection");
                    break;
                }
                byte[] data = new byte[len];
                dis.readFully(data);
                queue.put(new ClientBytes(data, socket));
            }
        } catch (Exception e) {
            if (running.get()) System.err.println("TCPReceiver error: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (Exception ignored) {}
        }
    }
}