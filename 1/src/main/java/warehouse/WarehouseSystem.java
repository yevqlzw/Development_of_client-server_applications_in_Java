package warehouse;

import pipeline.*;
import pipeline.enums.ComponentType;
import protocol.Package;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WarehouseSystem {
    private final ExecutorService executor;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Warehouse warehouse = new Warehouse();

    private final BlockingQueue<byte[]> q1 = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<Package> q2 = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<Package> q3 = new LinkedBlockingQueue<>(100);
    private final BlockingQueue<byte[]> q4 = new LinkedBlockingQueue<>(100);

    private final int receiverCount;
    private final int decryptorCount;
    private final int processorCount;
    private final int encryptorCount;
    private final int senderCount;

    public WarehouseSystem(int receivers, int decryptors, int processors, int encryptors, int senders) {
        this.receiverCount = receivers;
        this.decryptorCount = decryptors;
        this.processorCount = processors;
        this.encryptorCount = encryptors;
        this.senderCount = senders;

        int totalThreads = receivers + decryptors + processors + encryptors + senders;
        this.executor = Executors.newFixedThreadPool(totalThreads);

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

    public void start() {
        System.out.println("Warehouse system started (R:" + receiverCount +
                ", D:" + decryptorCount +
                ", P:" + processorCount +
                ", E:" + encryptorCount +
                ", S:" + senderCount + ")\n");

        for (int i = 0; i < receiverCount; i++) {
            executor.execute(new FakeReceiver(q1, running, ComponentType.RECEIVER, i + 1));
        }
        for (int i = 0; i < decryptorCount; i++) {
            executor.execute(new Decryptor(q1, q2, running, ComponentType.DECRYPTOR, i + 1));
        }
        for (int i = 0; i < processorCount; i++) {
            executor.execute(new Processor(q2, q3, running, warehouse, ComponentType.PROCESSOR, i + 1));
        }
        for (int i = 0; i < encryptorCount; i++) {
            executor.execute(new Encryptor(q3, q4, running, ComponentType.ENCRYPTOR, i + 1));
        }
        for (int i = 0; i < senderCount; i++) {
            executor.execute(new FakeSender(q4, running, ComponentType.SENDER, i + 1));
        }
    }

    public void stop() {
        System.out.println("\nStopping system...");
        running.set(false);
        executor.shutdownNow();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("Warehouse system stopped");
    }

    public static void main(String[] args) {
        WarehouseSystem system = new WarehouseSystem(5, 2, 4, 3, 5);
        system.start();
        try { Thread.sleep(15000); } catch (InterruptedException e) {}
        system.stop();
    }
}