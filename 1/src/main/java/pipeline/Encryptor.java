package pipeline;

import pipeline.enums.ComponentType;
import protocol.Encoder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Encryptor implements Runnable {
    private final BlockingQueue<ClientPackage> inputQueue;
    private final BlockingQueue<ClientBytes> outputQueue;
    private final AtomicBoolean running;
    private final Encoder encoder = new Encoder();
    private final String name;

    public Encryptor(BlockingQueue<ClientPackage> inputQueue,
                     BlockingQueue<ClientBytes> outputQueue,
                     AtomicBoolean running,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.name = type.getName(id);
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ClientPackage cap = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (cap == null) continue;
                byte[] encrypted = encoder.encode(cap.getPackage());
                outputQueue.put(new ClientBytes(encrypted, cap.getClientId()));
                System.out.println("[" + name + "] Encrypted");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[" + name + "] Error: " + e.getMessage());
            }
        }
    }
}