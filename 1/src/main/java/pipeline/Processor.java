package pipeline;

import pipeline.enums.CommandType;
import pipeline.enums.ComponentType;
import protocol.Message;
import protocol.Package;
import warehouse.Warehouse;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Processor implements Runnable {
    private final BlockingQueue<ClientPackage> inputQueue;
    private final BlockingQueue<ClientPackage> outputQueue;
    private final AtomicBoolean running;
    private final Warehouse warehouse;
    private final String name;

    public Processor(BlockingQueue<ClientPackage> inputQueue,
                     BlockingQueue<ClientPackage> outputQueue,
                     AtomicBoolean running,
                     Warehouse warehouse,
                     ComponentType type, int id) {
        this.inputQueue = inputQueue;
        this.outputQueue = outputQueue;
        this.running = running;
        this.warehouse = warehouse;
        this.name = type.getName(id);
    }

    private Message processCommand(Message request) {
        String[] parts = request.getMessage().split(" ");
        try {
            CommandType cmd = CommandType.fromCode(request.getcType());
            switch (cmd) {
                case GET_QUANTITY:
                    int id = Integer.parseInt(parts[0]);
                    int qty = warehouse.getQuantity(id);
                    String prodName = warehouse.getProductName(id);
                    return new Message(request.getcType(), request.getbUserId(),
                            (prodName != null ? prodName : "ID:" + id) + ": " + qty);
                case REMOVE_FROM_STOCK:
                    int pid = Integer.parseInt(parts[0]), amt = Integer.parseInt(parts[1]);
                    boolean ok = warehouse.removeQuantity(pid, amt);
                    return new Message(request.getcType(), request.getbUserId(), ok ? "OK" : "FAILED");
                case DEPOSIT:
                    int pid2 = Integer.parseInt(parts[0]), amt2 = Integer.parseInt(parts[1]);
                    warehouse.addQuantity(pid2, amt2);
                    return new Message(request.getcType(), request.getbUserId(), "Deposited");
                case ADD_GROUP:
                    warehouse.createGroup(Integer.parseInt(parts[0]));
                    return new Message(request.getcType(), request.getbUserId(), "Group created");
                case ADD_PRODUCT:
                    int gid = Integer.parseInt(parts[0]), prid = Integer.parseInt(parts[1]);
                    String name = parts.length > 2 ? parts[2] : "Product-" + prid;
                    boolean added = warehouse.addProductNameToGroup(gid, prid, name);
                    return new Message(request.getcType(), request.getbUserId(), added ? "Product added" : "Group not found");
                case SET_PRICE:
                    int prId = Integer.parseInt(parts[0]);
                    double price = Double.parseDouble(parts[1]);
                    warehouse.setPrice(prId, price);
                    return new Message(request.getcType(), request.getbUserId(), "Price set");
                default:
                    return new Message(request.getcType(), request.getbUserId(), "Unknown command");
            }
        } catch (Exception e) {
            return new Message(request.getcType(), request.getbUserId(), "Error: " + e.getMessage());
        }
    }

    @Override
    public void run() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ClientPackage cap = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (cap == null) continue;
                Package reqPkg = cap.getPackage();
                Message respMsg = processCommand(reqPkg.getMessage());
                Package respPkg = new Package((byte) 2, reqPkg.getbPktId(), respMsg);
                outputQueue.put(new ClientPackage(respPkg, cap.getClientId()));
                System.out.println("[" + name + "] Processed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}