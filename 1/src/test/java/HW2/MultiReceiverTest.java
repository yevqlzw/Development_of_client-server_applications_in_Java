package HW2;

import org.junit.jupiter.api.Test;
import pipeline.*;
import pipeline.fake_implementation.FakeReceiver;
import pipeline.fake_implementation.FakeSender;
import pipeline.enums.ComponentType;
import protocol.MyCipher;
import warehouse.ProductManager;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

public class MultiReceiverTest {

    @Test
    void testMultipleReceivers() throws InterruptedException {
        MyCipher.setTestKey();

        BlockingQueue<ClientBytes> q1 = new LinkedBlockingQueue<>();
        BlockingQueue<ClientPackage> q2 = new LinkedBlockingQueue<>();
        BlockingQueue<ClientPackage> q3 = new LinkedBlockingQueue<>();
        BlockingQueue<ClientBytes> q4 = new LinkedBlockingQueue<>(); // для зашифрованих відповідей
        AtomicBoolean running = new AtomicBoolean(true);

        ProductManager productManager = new ProductManager();
        productManager.createGroup(1);
        productManager.createGroup(2);
        productManager.addProductToGroup(1, 10, "Shirt");
        productManager.addProductToGroup(1, 11, "Jeans");
        productManager.addProductToGroup(2, 20, "Jacket");
        productManager.addProductToGroup(2, 21, "Socks");
        productManager.addQuantity(10, 100);
        productManager.addQuantity(11, 200);
        productManager.addQuantity(20, 500);
        productManager.addQuantity(21, 300);

        for (int i = 0; i < 3; i++) {
            new Thread(new FakeReceiver(q1, running, ComponentType.RECEIVER, i + 1)).start();
        }

        for (int i = 0; i < 2; i++) {
            new Thread(new Decryptor(q1, q2, running, ComponentType.DECRYPTOR, i + 1)).start();
        }

        for (int i = 0; i < 3; i++) {
            new Thread(new Processor(q2, q3, running, productManager, ComponentType.PROCESSOR, i + 1)).start();
        }

        for (int i = 0; i < 2; i++) {
            new Thread(new Encryptor(q3, q4, running, ComponentType.ENCRYPTOR, i + 1)).start();
        }

        for (int i = 0; i < 2; i++) {
            new Thread(new FakeSender(q4, running, ComponentType.SENDER, i + 1)).start();
        }

        Thread.sleep(4000);
        running.set(false);
        Thread.sleep(500);

        System.out.println("\n Results");
        System.out.println("Shirt: " + productManager.getQuantity(10));
        System.out.println("Jeans: " + productManager.getQuantity(11));
        System.out.println("Jacket: " + productManager.getQuantity(20));
        System.out.println("Socks: " + productManager.getQuantity(21));
        System.out.println("Responses in processQueue: " + q3.size());

        assertFalse(q3.isEmpty());
    }
}