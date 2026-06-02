package pipeline.tcp;

import pipeline.ClientBytes;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPSender implements Runnable {
    private final BlockingQueue<ClientBytes> queue;
    private final AtomicBoolean running;
    private final ConcurrentHashMap<Socket, DataOutputStream> streams = new ConcurrentHashMap<>();

    public TCPSender(BlockingQueue<ClientBytes> q, AtomicBoolean r) {
        queue = q;
        running = r;
    }

    public void registerClient(Socket s) throws IOException {
        streams.put(s, new DataOutputStream(s.getOutputStream()));
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ClientBytes cab = queue.take();
                Socket socket = (Socket) cab.getClientId();
                DataOutputStream dos = streams.get(socket);
                if (dos != null) {
                    try {
                        byte[] data = cab.getData();
                        dos.writeInt(data.length);
                        dos.write(data);
                        dos.flush();
                    } catch (IOException e) {
                        streams.remove(socket);
                        try { socket.close(); } catch (IOException ignored) {}
                        System.err.println("TCP Sender: removed dead socket " + socket.getRemoteSocketAddress());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}