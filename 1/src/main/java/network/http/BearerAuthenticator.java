package network.http;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.net.httpserver.Authenticator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;

import java.util.List;

public class BearerAuthenticator extends Authenticator {
    private final AuthService authService;

    public BearerAuthenticator(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public Result authenticate(HttpExchange exchange) {
        List<String> authHeaders = exchange.getRequestHeaders().get("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return new Failure(401);
        }
        String authHeader = authHeaders.get(0);
        if (!authHeader.startsWith("Bearer ")) {
            return new Failure(401);
        }
        String token = authHeader.substring(7);
        try {
            String username = authService.validateToken(token);
            return new Success(new HttpPrincipal(username, "user"));
        } catch (JWTVerificationException e) {
            return new Failure(403);
        }
    }
}