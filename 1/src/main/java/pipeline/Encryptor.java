package pipeline;

import pipeline.enums.ComponentType;
import pipeline.interfaces.EncryptorInterface;
import protocol.Encoder;
import protocol.Package;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Encryptor implements EncryptorInterface, Runnable {
    private final BlockingQueue<Package> inputQueue;
    private final BlockingQueue<byte[]> outputQueue;
    private final AtomicBoolean running;
    private final Encoder encoder = new Encoder();
    private final String name;

    public Encryptor(BlockingQueue<Package> inputQueue,
                     BlockingQueue<byte[]> outputQueue,
                     AtomicBoolean running,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.name = type.getName(id);
    }

    @Override
    public byte[] encrypt(Package message) {
        return encoder.encode(message);
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Package pack = inputQueue.take();
                byte[] encrypted = encrypt(pack);
                outputQueue.put(encrypted);
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