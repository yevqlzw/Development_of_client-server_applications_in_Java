package pipeline;

import pipeline.enums.ComponentType;
import protocol.Decoder;
import protocol.Package;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Decryptor implements Runnable {
    private final BlockingQueue<ClientBytes> inputQueue;
    private final BlockingQueue<ClientPackage> outputQueue;
    private final AtomicBoolean running;
    private final Decoder decoder = new Decoder();
    private final String name;

    public Decryptor(BlockingQueue<ClientBytes> inputQueue,
                     BlockingQueue<ClientPackage> outputQueue,
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
                ClientBytes cab = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (cab == null) continue;
                Package pkg = decoder.decode(cab.getData());
                outputQueue.put(new ClientPackage(pkg, cab.getClientId()));
                System.out.println("[" + name + "] Decrypted");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.println("[" + name + "] Error: " + e.getMessage());
            }
        }
    }
}