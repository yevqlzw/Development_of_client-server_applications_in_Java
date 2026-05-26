package HW2;

import org.junit.jupiter.api.Test;
import warehouse.Warehouse;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

public class ConcurrentUpdateTest {

    @Test
    void testConcurrentDepositAndRemove() throws InterruptedException {
        Warehouse warehouse = new Warehouse();
        warehouse.createGroup(1);
        warehouse.addProductNameToGroup(1, 10, "Test");
        warehouse.addQuantity(10, 0);

        int threads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(20);
        CountDownLatch latch = new CountDownLatch(threads * 2);
        AtomicInteger added = new AtomicInteger(0);
        AtomicInteger removed = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.execute(() -> {
                warehouse.addQuantity(10, 1);
                added.incrementAndGet();
                latch.countDown();
            });
            executor.execute(() -> {
                boolean ok = warehouse.removeQuantity(10, 1);
                if (ok) removed.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        int stock = warehouse.getQuantity(10);

        System.out.println("Added: " + added.get());
        System.out.println("Removed: " + removed.get());
        System.out.println("Stock: " + stock);

        assertEquals(added.get() - removed.get(), stock);
    }
}