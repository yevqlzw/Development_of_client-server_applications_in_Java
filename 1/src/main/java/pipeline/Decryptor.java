package pipeline;

import pipeline.enums.ComponentType;
import pipeline.interfaces.DecryptorInterface;
import protocol.Decoder;
import protocol.Package;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Decryptor implements DecryptorInterface, Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final BlockingQueue<Package> outputQueue;
    private final AtomicBoolean running;
    private final Decoder decoder = new Decoder();
    private final String name;

    public Decryptor(BlockingQueue<byte[]> inputQueue,
                     BlockingQueue<Package> outputQueue,
                     AtomicBoolean running,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.name = type.getName(id);
    }

    @Override
    public void decrypt(byte[] message) {
        try {
            Package pack = decoder.decode(message);
            outputQueue.put(pack);
            System.out.println("[" + name + "] Decrypted");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] message = inputQueue.take();
                decrypt(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}