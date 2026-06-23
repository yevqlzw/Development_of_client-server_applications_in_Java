package network.udp;

import database.ProductDbService;
import network.Utils;
import pipeline.*;
import pipeline.enums.ComponentType;
import pipeline.udp.UDPReceiver;
import pipeline.udp.UDPSender;
import warehouse.ProductService;

import java.net.DatagramSocket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerUDP {
    private final int port;
    private final ProductService productManager;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private DatagramSocket socket;
    private ExecutorService executor;
    private final LinkedBlockingQueue<ClientBytes> rawQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> decryptQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> processQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientBytes> encryptQueue = new LinkedBlockingQueue<>();

    public StoreServerUDP(int port) {
        this.port = port;
        this.productManager = new ProductDbService();
        initWarehouse();
    }

    private void initWarehouse() {
        if (!productManager.groupExists(1)) {
            productManager.createGroup(1);
            productManager.createGroup(2);
            productManager.addProductToGroup(1, 10, "Shirt");
            productManager.addProductToGroup(1, 11, "Jeans");
            productManager.addProductToGroup(2, 20, "Jacket");
            productManager.addProductToGroup(2, 21, "Socks");
            productManager.addQuantity(10, 100);
            productManager.addQuantity(11, 200);
            productManager.addQuantity(20, 500);
            productManager.addQuantity(21, 300);
            productManager.setPrice(10, 45.50);
            productManager.setPrice(11, 30.00);
            productManager.setPrice(20, 15.00);
            productManager.setPrice(21, 35.00);
        }
    }

    public void start() throws Exception {
        socket = new DatagramSocket(port);
        executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 2; i++) executor.submit(new Decryptor(rawQueue, decryptQueue, running, ComponentType.DECRYPTOR, i+1));
        for (int i = 0; i < 4; i++) executor.submit(new Processor(decryptQueue, processQueue, running, productManager, ComponentType.PROCESSOR, i+1));
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
        Thread.sleep(40000);
        server.stop();
    }
}