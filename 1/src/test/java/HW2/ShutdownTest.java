package HW2;

import org.junit.jupiter.api.Test;
import pipeline.*;
import pipeline.fake_implementation.FakeReceiver;
import pipeline.enums.ComponentType;
import protocol.*;
import warehouse.Warehouse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

public class ShutdownTest {

    @Test
    void testAllThreadsStop() throws InterruptedException {
        MyCipher.setTestKey();

        BlockingQueue<ClientBytes> q1 = new LinkedBlockingQueue<>();
        BlockingQueue<ClientPackage> q2 = new LinkedBlockingQueue<>();
        BlockingQueue<ClientPackage> q3 = new LinkedBlockingQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);

        Warehouse warehouse = new Warehouse();
        warehouse.createGroup(1);
        warehouse.addProductNameToGroup(1, 10, "Test");
        warehouse.addQuantity(10, 0);

        ExecutorService executor = Executors.newFixedThreadPool(7);

        for (int i = 0; i < 3; i++) {
            executor.execute(new FakeReceiver(q1, running, ComponentType.RECEIVER, i + 1));
        }
        for (int i = 0; i < 2; i++) {
            executor.execute(new Decryptor(q1, q2, running, ComponentType.DECRYPTOR, i + 1));
        }
        for (int i = 0; i < 2; i++) {
            executor.execute(new Processor(q2, q3, running, warehouse, ComponentType.PROCESSOR, i + 1));
        }

        Thread.sleep(2000);
        running.set(false);
        executor.shutdownNow();

        boolean stopped = executor.awaitTermination(3, TimeUnit.SECONDS);
        assertTrue(stopped);
    }
}