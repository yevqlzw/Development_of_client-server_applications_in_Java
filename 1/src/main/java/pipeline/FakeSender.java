package pipeline;

import pipeline.enums.ComponentType;
import pipeline.interfaces.SenderInterface;

import java.net.InetAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class FakeSender implements SenderInterface, Runnable {
    private final BlockingQueue<byte[]> inputQueue;
    private final AtomicBoolean isRunning;
    private final String name;

    public FakeSender(BlockingQueue<byte[]> inputQueue, AtomicBoolean isRunning, ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.isRunning =isRunning;
        this.name = type.getName(id);
    }

    @Override
    public void sendMessage(byte[] mess, InetAddress target) {
        System.out.println("[" + name + "] Response sent");
    }

    @Override
    public void run() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            try {
                byte[] message = inputQueue.take();
                sendMessage(message, InetAddress.getLoopbackAddress());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}