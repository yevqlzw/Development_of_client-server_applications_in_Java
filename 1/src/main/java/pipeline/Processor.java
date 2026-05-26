package pipeline;

import pipeline.enums.CommandType;
import pipeline.enums.ComponentType;
import pipeline.interfaces.ProcessorInterface;
import protocol.Message;
import protocol.Package;
import warehouse.Warehouse;

import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements ProcessorInterface, Runnable {
    private final BlockingQueue<Package> inputQueue;
    private final BlockingQueue<Package> outputQueue;
    private final AtomicBoolean running;
    private final Warehouse warehouse;
    private final String name;

    public Processor(BlockingQueue<Package> inputQueue,
                     BlockingQueue<Package> outputQueue,
                     AtomicBoolean running,
                     Warehouse warehouse,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.warehouse = warehouse;
        this.name = type.getName(id);
    }

    @Override
    public void process(Message message) {
        String[] parts = message.getMessage().split(" ");

        try {
            CommandType cmd = CommandType.fromCode(message.getcType());

            switch (cmd) {
                case GET_QUANTITY: {
                    int id = Integer.parseInt(parts[0]);
                    int qty = warehouse.getQuantity(id);
                    String n = warehouse.getProductName(id);
                    if (n == null) n = "ID:" + id;
                    System.out.println("[" + name + "] " + n + ": " + qty + " pcs");
                    break;
                }
                case REMOVE_FROM_STOCK: {
                    int id = Integer.parseInt(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    String n = warehouse.getProductName(id);
                    if (n == null) n = "ID:" + id;
                    boolean ok = warehouse.removeQuantity(id, amount);
                    int after = warehouse.getQuantity(id);
                    System.out.println("[" + name + "] " + n + " -" + amount + " -> " + after + (ok ? "" : " FAILED"));
                    break;
                }
                case DEPOSIT: {
                    int id = Integer.parseInt(parts[0]);
                    int amount = Integer.parseInt(parts[1]);
                    String n = warehouse.getProductName(id);
                    if (n == null) n = "ID:" + id;
                    warehouse.addQuantity(id, amount);
                    int after = warehouse.getQuantity(id);
                    System.out.println("[" + name + "] " + n + " +" + amount + " -> " + after);
                    break;
                }
                case ADD_GROUP: {
                    int id = Integer.parseInt(parts[0]);
                    warehouse.createGroup(id);
                    System.out.println("[" + name + "] Group #" + id + " created");
                    break;
                }
                case ADD_PRODUCT: {
                    int groupID = Integer.parseInt(parts[0]);
                    int productID = Integer.parseInt(parts[1]);
                    String pname = parts.length > 2 ? parts[2] : "Product-" + productID;
                    boolean ok = warehouse.addProductNameToGroup(groupID, productID, pname);
                    System.out.println("[" + name + "] " + pname + " -> group " + groupID + (ok ? "" : " FAILED"));
                    break;
                }
                case SET_PRICE: {
                    int id = Integer.parseInt(parts[0]);
                    double price = Double.parseDouble(parts[1].replace(',', '.'));
                    warehouse.setPrice(id, price);
                    String n = warehouse.getProductName(id);
                    if (n == null) n = "ID:" + id;
                    System.out.printf("[%s] %s: price %.2f%n", name, n, price);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[" + name + "] Error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Package pack = inputQueue.take();
                process(pack.getMessage());

                Message resp = new Message(pack.getMessage().getcType(), pack.getMessage().getbUserId(), "OK");
                Package respPack = new Package();
                respPack.setbSrc((byte) 2);
                respPack.setbPktId(pack.getbPktId());
                respPack.setMessage(resp);
                outputQueue.put(respPack);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}