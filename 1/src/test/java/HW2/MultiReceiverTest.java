package HW2;

import org.junit.jupiter.api.Test;
import pipeline.*;
import pipeline.enums.ComponentType;
import protocol.*;
import protocol.Package;
import warehouse.Warehouse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

public class MultiReceiverTest {

    @Test
    void testMultipleReceivers() throws InterruptedException {
        MyCipher.setTestKey();

        BlockingQueue<byte[]> q1 = new LinkedBlockingQueue<>();
        BlockingQueue<Package> q2 = new LinkedBlockingQueue<>();
        BlockingQueue<Package> q3 = new LinkedBlockingQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);

        Warehouse warehouse = new Warehouse();
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

        for (int i = 0; i < 3; i++) {
            new Thread(new FakeReceiver(q1, running, ComponentType.RECEIVER, i + 1)).start();
        }

        for (int i = 0; i < 2; i++) {
            new Thread(new Decryptor(q1, q2, running, ComponentType.DECRYPTOR, i + 1)).start();
        }

        for (int i = 0; i < 3; i++) {
            new Thread(new Processor(q2, q3, running, warehouse, ComponentType.PROCESSOR, i + 1)).start();
        }

        Thread.sleep(4000);
        running.set(false);
        Thread.sleep(500);

        System.out.println("\n Results");
        System.out.println("Shirt: " + warehouse.getQuantity(10));
        System.out.println("Jeans: " + warehouse.getQuantity(11));
        System.out.println("Jacket: " + warehouse.getQuantity(20));
        System.out.println("Socks: " + warehouse.getQuantity(21));
        System.out.println("Responses: " + q3.size());

        assertFalse(q3.isEmpty());    }
}