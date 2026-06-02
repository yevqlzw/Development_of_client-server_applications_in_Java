package TCP_UDP;

import network.Utils;
import network.tcp.StoreClientTCP;
import network.tcp.StoreServerTCP;
import org.junit.jupiter.api.*;
import pipeline.enums.CommandType;
import protocol.MyCipher;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TcpTest {

    private StoreServerTCP server;
    private ExecutorService serverExecutor;

    @BeforeAll
    void startServer() throws Exception {
        MyCipher.setTestKey();
        server = new StoreServerTCP(Utils.TCP_PORT);
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
                StoreClientTCP client = null;
                try {
                    client = new StoreClientTCP("localhost", Utils.TCP_PORT);
                    client.connect();
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
        assertTrue(latch.await(15, TimeUnit.SECONDS), "Not all clients finished");
        clientPool.shutdown();
        for (int i = 0; i < clientCount; i++) {
            String resp = results.get(i);
            assertNotNull(resp, "Client " + i + " has no response");
            assertTrue(resp.contains("Shirt") || resp.contains("92") || resp.contains("100"),
                    "Client " + i + " got: " + resp);
        }
    }

    @Test
    void testReconnectAfterServerRestart() throws Exception {
        StoreClientTCP client = new StoreClientTCP("localhost", Utils.TCP_PORT);
        try {
            client.connect();
            String first = client.sendCommand(CommandType.GET_QUANTITY, "10");
            assertTrue(first.contains("Shirt"));

            server.stop();
            Thread.sleep(500);

            server = new StoreServerTCP(Utils.TCP_PORT);
            serverExecutor.submit(() -> {
                try {
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            Thread.sleep(1500);

            String second = client.sendCommand(CommandType.GET_QUANTITY, "10");
            assertTrue(second.contains("Shirt"));
        } finally {
            client.close();
        }
    }
}