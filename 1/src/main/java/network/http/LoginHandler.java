package network.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LoginHandler implements HttpHandler {
    private final AuthService authService;
    private final ObjectMapper mapper;

    public LoginHandler(AuthService authService, ObjectMapper mapper) {
        this.authService = authService;
        this.mapper = mapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            LoginRequest req = mapper.readValue(is, LoginRequest.class);
            if (authService.authenticate(req.getUsername(), req.getPassword())) {
                String token = authService.generateToken(req.getUsername());
                String json = mapper.writeValueAsString(new LoginResponse(token));
                sendResponse(exchange, 200, json);
            } else {
                sendError(exchange, 401, "Invalid credentials");
            }
        } catch (Exception e) {
            sendError(exchange, 400, "Bad request");
        }
    }

    private void sendResponse(HttpExchange exchange, int status, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }
        exchange.close();
    }

    private void sendError(HttpExchange exchange, int status, String msg) throws IOException {
        sendResponse(exchange, status, "{\"error\":\"" + msg + "\"}");
    }

    private static class LoginRequest {
        private String username;
        private String password;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    private static class LoginResponse {
        private String token;
        public LoginResponse(String token) { this.token = token; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}