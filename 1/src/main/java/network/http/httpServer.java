package network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import database.ProductDbService;
import warehouse.ProductService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class httpServer {
    private final HttpServer server;
    private final ProductService productService;
    private final AuthService authService;
    private final ObjectMapper mapper = new ObjectMapper();

    public httpServer(int port) throws IOException {
        this.productService = new ProductDbService();
        this.authService = new AuthService();
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        createContexts();
    }

    private void createContexts() {
        server.createContext("/login", new LoginHandler(authService, mapper));

        var productHandler = new ProductHandler(productService, mapper);
        var productContext = server.createContext("/products", productHandler);
        productContext.setAuthenticator(new BearerAuthenticator(authService));

        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
    }

    public void start() {
        server.start();
        System.out.println("HTTP Server started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }

    public ProductService getProductService() {
        return productService;
    }

    public static void main(String[] args) throws Exception {
        httpServer server = new httpServer(8082);
        server.start();
        Thread.currentThread().join();
    }
}