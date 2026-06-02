package pipeline.udp;

import pipeline.ClientBytes;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class UDPSender implements Runnable {
    private final BlockingQueue<ClientBytes> queue;
    private final DatagramSocket socket;
    private final AtomicBoolean running;

    public UDPSender(BlockingQueue<ClientBytes> q, DatagramSocket s, AtomicBoolean r) {
        queue = q;
        socket = s;
        running = r;
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ClientBytes cab = queue.take();
                InetSocketAddress addr = (InetSocketAddress) cab.getClientId();
                byte[] data = cab.getData();
                DatagramPacket packet = new DatagramPacket(data, data.length, addr.getAddress(), addr.getPort());
                synchronized (socket) {
                    socket.send(packet);
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