package pipeline.udp;

import pipeline.ClientBytes;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPReceiver implements Runnable {
    private final DatagramSocket socket;
    private final BlockingQueue<ClientBytes> queue;
    private final AtomicBoolean running;
    private final byte[] buf = new byte[65536];
    public UDPReceiver(DatagramSocket s, BlockingQueue<ClientBytes> q, AtomicBoolean r) {
        socket = s; queue = q; running = r;
    }
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                socket.receive(p);
                byte[] data = new byte[p.getLength()];
                System.arraycopy(p.getData(), p.getOffset(), data, 0, p.getLength());
                queue.put(new ClientBytes(data, new InetSocketAddress(p.getAddress(), p.getPort())));
            } catch (Exception e) { if (running.get()) e.printStackTrace(); }
        }
    }
}