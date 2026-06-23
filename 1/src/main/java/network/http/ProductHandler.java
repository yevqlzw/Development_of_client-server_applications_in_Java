package network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import warehouse.Product;
import warehouse.ProductService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class ProductHandler implements HttpHandler {
    private final ProductService productService;
    private final ObjectMapper mapper;

    public ProductHandler(ProductService productService, ObjectMapper mapper) {
        this.productService = productService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if (method.equals("GET") && path.matches("^/products/\\d+$")) {
                handleGet(exchange, extractId(path));
            } else if (method.equals("PUT") && path.equals("/products")) {
                handlePut(exchange);
            } else if (method.equals("POST") && path.matches("^/products/\\d+$")) {
                handlePost(exchange, extractId(path));
            } else if (method.equals("DELETE") && path.matches("^/products/\\d+$")) {
                handleDelete(exchange, extractId(path));
            } else {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Internal error: " + e.getMessage());
        }
    }

    private int extractId(String path) {
        String[] parts = path.split("/");
        return Integer.parseInt(parts[parts.length - 1]);
    }

    private void handleGet(HttpExchange exchange, int id) throws IOException {
        Product p = productService.getProduct(id);
        if (p == null) {
            exchange.sendResponseHeaders(404, -1);
        } else {
            String json = mapper.writeValueAsString(p);
            sendResponse(exchange, 200, json);
        }
        exchange.close();
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            CreateRequest req = mapper.readValue(is, CreateRequest.class);

            Collection<Product> all = productService.getAllProducts();
            boolean exists = all.stream().anyMatch(p -> p.getName().equalsIgnoreCase(req.getName()));
            if (exists) {
                sendError(exchange, 409, "Product name already exists");
                return;
            }
            if (!productService.groupExists(req.getGroupId())) {
                sendError(exchange, 400, "Group does not exist");
                return;
            }
            Product created = productService.createProduct(
                    req.getName(), req.getGroupId(),
                    req.getQuantity(), req.getPrice()
            );
            String json = mapper.writeValueAsString(created);
            exchange.getResponseHeaders().set("Location", "/products/" + created.getId());
            sendResponse(exchange, 201, json);
        } catch (IllegalArgumentException e) {
            sendError(exchange, 400, e.getMessage());
        } finally {
            exchange.close();
        }
    }

    private void handlePost(HttpExchange exchange, int id) throws IOException {
        Product existing = productService.getProduct(id);
        if (existing == null) {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
            return;
        }
        try (InputStream is = exchange.getRequestBody()) {
            UpdateRequest req = mapper.readValue(is, UpdateRequest.class);
            boolean updated = productService.updateProduct(
                    id, req.getName(), req.getGroupId(),
                    req.getQuantity(), req.getPrice()
            );
            if (updated) {
                Product updatedProduct = productService.getProduct(id);
                String json = mapper.writeValueAsString(updatedProduct);
                sendResponse(exchange, 200, json);
            } else {
                sendError(exchange, 400, "Update failed");
            }
        } finally {
            exchange.close();
        }
    }

    private void handleDelete(HttpExchange exchange, int id) throws IOException {
        boolean deleted = productService.deleteProduct(id);
        exchange.sendResponseHeaders(deleted ? 204 : 404, -1);
        exchange.close();
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        String json = "{\"error\":\"" + msg + "\"}";
        sendResponse(exchange, status, json);
    }

    private static class CreateRequest {
        private String name;
        private int groupId;
        private int quantity;
        private double price;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getGroupId() { return groupId; }
        public void setGroupId(int groupId) { this.groupId = groupId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }
    }

    private static class UpdateRequest {
        private String name;
        private Integer groupId;
        private Integer quantity;
        private Double price;
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getGroupId() { return groupId; }
        public void setGroupId(Integer groupId) { this.groupId = groupId; }
        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
    }
}