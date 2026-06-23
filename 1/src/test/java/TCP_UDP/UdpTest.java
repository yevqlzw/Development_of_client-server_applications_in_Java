package TCP_UDP;

import java.nio.file.Files;
import java.nio.file.Paths;

import network.Utils;
import network.udp.StoreClientUDP;
import network.udp.StoreServerUDP;
import org.junit.jupiter.api.*;
import pipeline.enums.CommandType;
import protocol.MyCipher;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UdpTest {

    private StoreServerUDP server;
    private ExecutorService serverExecutor;

    @BeforeAll
    void startServer() throws Exception {
        Files.deleteIfExists(Paths.get("warehouse.db"));
        MyCipher.setTestKey();
        server = new StoreServerUDP(Utils.UDP_PORT);
        serverExecutor = Executors.newSingleThreadExecutor();
        serverExecutor.submit(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1500);
    }

    @AfterAll
    void stopServer() throws InterruptedException {
        server.stop();
        serverExecutor.shutdownNow();
    }

    @Test
    void testMultipleClients() throws Exception {
        int clientCount = 5;
        ExecutorService clientPool = Executors.newFixedThreadPool(clientCount);
        CountDownLatch latch = new CountDownLatch(clientCount);
        ConcurrentHashMap<Integer, String> results = new ConcurrentHashMap<>();

        for (int i = 0; i < clientCount; i++) {
            final int clientId = i;
            clientPool.submit(() -> {
                StoreClientUDP client = null;
                try {
                    client = new StoreClientUDP("localhost", Utils.UDP_PORT);
                    String response = client.sendCommand(CommandType.GET_QUANTITY, "10");
                    results.put(clientId, response);
                } catch (Exception e) {
                    results.put(clientId, "ERROR: " + e.getMessage());
                } finally {
                    if (client != null) client.close();
                    latch.countDown();
                }
            });
        }
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Not all UDP clients finished");
        clientPool.shutdown();
        for (int i = 0; i < clientCount; i++) {
            String resp = results.get(i);
            assertNotNull(resp, "Client " + i + " no response");
            assertTrue(resp.contains("Shirt"), "UDP client " + i + " failed: " + resp);
        }
    }

    @Test
    void testUdpRetryOnPacketLoss() throws Exception {
        StoreClientUDP client1 = null;
        try {
            client1 = new StoreClientUDP("localhost", Utils.UDP_PORT);
            String response = client1.sendCommand(CommandType.GET_QUANTITY, "10");
            assertTrue(response.contains("Shirt"));
        } finally {
            if (client1 != null) client1.close();
        }

        server.stop();
        Thread.sleep(500);

        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<String> future = exec.submit(() -> {
            StoreClientUDP client = null;
            try {
                client = new StoreClientUDP("localhost", Utils.UDP_PORT);
                return client.sendCommand(CommandType.GET_QUANTITY, "10");
            } finally {
                if (client != null) client.close();
            }
        });

        Thread.sleep(2000);

        server = new StoreServerUDP(Utils.UDP_PORT);
        serverExecutor.submit(() -> {
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        Thread.sleep(1500);

        String response = future.get(10, TimeUnit.SECONDS);
        assertTrue(response.contains("Shirt"), "UDP retry failed: " + response);
        exec.shutdown();
    }
}