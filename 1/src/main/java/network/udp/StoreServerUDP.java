package network.udp;

import network.Utils;
import pipeline.*;
import pipeline.enums.ComponentType;
import pipeline.udp.UDPReceiver;
import pipeline.udp.UDPSender;
import warehouse.Warehouse;
import java.net.DatagramSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerUDP {
    private final int port;
    private final Warehouse warehouse;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket socket;
    private ExecutorService executor;
    private final LinkedBlockingQueue<ClientBytes> rawQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> decryptQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> processQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientBytes> encryptQueue = new LinkedBlockingQueue<>();

    public StoreServerUDP(int port) {
        this.port = port;
        this.warehouse = new Warehouse();
        initWarehouse();
    }

    private void initWarehouse() {
        warehouse.createGroup(1);
        warehouse.createGroup(2);
        warehouse.addProductNameToGroup(1, 10, "Shirt");
        warehouse.addProductNameToGroup(1, 11, "Jeans");
        warehouse.addProductNameToGroup(2, 20, "Jacket");
        warehouse.addProductNameToGroup(2, 21, "Socks");
        warehouse.addQuantity(10, 100);
        warehouse.addQuantity(11, 200);
        warehouse.addQuantity(20, 500);
        warehouse.addQuantity(21, 300);
        warehouse.setPrice(10, 45.50);
        warehouse.setPrice(11, 30.00);
        warehouse.setPrice(20, 15.00);
        warehouse.setPrice(21, 35.00);
    }

    public void start() throws Exception {
        socket = new DatagramSocket(port);
        executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 2; i++) executor.submit(new Decryptor(rawQueue, decryptQueue, running, ComponentType.DECRYPTOR, i+1));
        for (int i = 0; i < 4; i++) executor.submit(new Processor(decryptQueue, processQueue, running, warehouse, ComponentType.PROCESSOR, i+1));
        for (int i = 0; i < 2; i++) executor.submit(new Encryptor(processQueue, encryptQueue, running, ComponentType.ENCRYPTOR, i+1));
        UDPSender sender = new UDPSender(encryptQueue, socket, running);
        for (int i = 0; i < 2; i++) executor.submit(sender);
        executor.submit(new UDPReceiver(socket, rawQueue, running));
        System.out.println("UDP Pipeline Server started on port " + port);
    }

    public void stop() throws InterruptedException {
        running.set(false);
        executor.shutdownNow();
        if (socket != null) socket.close();
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("UDP Server stopped");
    }

    public static void main(String[] args) throws Exception {
        StoreServerUDP server = new StoreServerUDP(Utils.UDP_PORT);
        server.start();
        Thread.sleep(60000);
        server.stop();
    }
}