package network.tcp;

import network.Utils;
import pipeline.*;
import pipeline.enums.ComponentType;
import pipeline.tcp.TCPReceiver;
import pipeline.tcp.TCPSender;
import warehouse.ProductManager;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class StoreServerTCP {
    private final int port;
    private final ProductManager productManager;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private ServerSocket serverSocket;
    private ExecutorService executor;
    private final LinkedBlockingQueue<ClientBytes> rawQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> decryptQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientPackage> processQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<ClientBytes> encryptQueue = new LinkedBlockingQueue<>();

    public StoreServerTCP(int port) {
        this.port = port;
        this.productManager = new ProductManager();
        initWarehouse();
    }

    private void initWarehouse() {
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

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < 2; i++) executor.submit(new Decryptor(rawQueue, decryptQueue, running, ComponentType.DECRYPTOR, i+1));
        for (int i = 0; i < 4; i++) executor.submit(new Processor(decryptQueue, processQueue, running, productManager, ComponentType.PROCESSOR, i+1));
        for (int i = 0; i < 2; i++) executor.submit(new Encryptor(processQueue, encryptQueue, running, ComponentType.ENCRYPTOR, i+1));
        TCPSender sender = new TCPSender(encryptQueue, running);
        for (int i = 0; i < 2; i++) executor.submit(sender);
        executor.submit(() -> {
            while (running.get()) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("TCP client connected: " + client.getRemoteSocketAddress());
                    sender.registerClient(client);
                    executor.submit(new TCPReceiver(client, rawQueue, running));
                } catch (Exception e) { if (running.get()) e.printStackTrace(); }
            }
        });
        System.out.println("TCP Pipeline Server started on port " + port);
    }

    public void stop() throws InterruptedException {
        running.set(false);
        executor.shutdownNow();
        if (serverSocket != null) try { serverSocket.close(); } catch (IOException ignored) {}
        executor.awaitTermination(5, TimeUnit.SECONDS);
        System.out.println("TCP Server stopped");
    }

    public static void main(String[] args) throws Exception {
        StoreServerTCP server = new StoreServerTCP(Utils.TCP_PORT);
        server.start();
        Thread.sleep(40000);
        server.stop();
    }
}