package pipeline.fake_implementation;

import pipeline.ClientBytes;
import pipeline.enums.CommandType;
import pipeline.enums.ComponentType;
import pipeline.interfaces.ReceiverInterface;
import protocol.Encoder;
import protocol.Message;
import protocol.Package;

import java.util.Locale;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class FakeReceiver implements ReceiverInterface, Runnable {
    private final BlockingQueue<ClientBytes> inputQueue;
    private final AtomicBoolean isRunning;
    private final Random random = new Random();
    private final String name;
    private static final AtomicLong packetIdCounter = new AtomicLong(0);

    private static final int[] PRODUCTS = {10, 11, 20, 21};
    private static final String[] NAMES = {"Shirt", "Jeans", "Jacket", "Socks"};
    private static final int[] GROUPS = {1, 2};

    public FakeReceiver(BlockingQueue<ClientBytes> inputQueue, AtomicBoolean isRunning, ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.isRunning = isRunning;
        this.name = type.getName(id);
    }

    @Override
    public void receiveMessage() {
        CommandType[] commands = CommandType.values();
        CommandType cmd = commands[random.nextInt(commands.length)];
        String message = "";
        String log = "";

        int idx = random.nextInt(PRODUCTS.length);
        int productID = PRODUCTS[idx];
        String productName = NAMES[idx];
        int groupID = GROUPS[random.nextInt(GROUPS.length)];

        switch (cmd) {
            case GET_QUANTITY:
                message = String.valueOf(productID);
                log = "GET_QUANTITY " + productName;
                break;

            case REMOVE_FROM_STOCK: {
                int amount = random.nextInt(10) + 1;
                message = productID + " " + amount;
                log = "REMOVE_FROM_STOCK " + productName + " " + amount;
                break;
            }

            case DEPOSIT: {
                int amount = random.nextInt(20) + 1;
                message = productID + " " + amount;
                log = "DEPOSIT " + productName + " " + amount;
                break;
            }

            case ADD_GROUP: {
                int gid = random.nextInt(100) + 10;
                message = String.valueOf(gid);
                log = "ADD_GROUP " + gid;
                break;
            }

            case ADD_PRODUCT: {
                int newID = random.nextInt(100) + 50;
                message = groupID + " " + newID + " Product-" + newID;
                log = "ADD_PRODUCT group=" + groupID + " id=" + newID;
                break;
            }

            case SET_PRICE: {
                double price = 10.0 + random.nextDouble() * 100;
                message = productID + " " + String.format(Locale.US, "%.2f", price);
                log = "SET_PRICE " + productName + " " + String.format("%.2f", price);
                break;
            }

            default:
                return;
        }

        System.out.println("[" + name + "] " + log);

        Package pack = new Package();
        pack.setbSrc((byte) 1);
        pack.setbPktId(packetIdCounter.incrementAndGet());
        pack.setMessage(new Message(cmd.getCode(), random.nextInt(1000), message));

        Encoder encoder = new Encoder();
        try {
            inputQueue.put(new ClientBytes(encoder.encode(pack), "fake-client"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void run() {
        while (isRunning.get() && !Thread.currentThread().isInterrupted()) {
            receiveMessage();
            try {
                Thread.sleep(random.nextInt(500) + 300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}