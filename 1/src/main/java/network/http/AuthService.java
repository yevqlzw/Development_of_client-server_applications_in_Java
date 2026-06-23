package network.http;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthService {
    private static final String SECRET = "my-very-very-very-secure-secret-key";
    private static final long EXPIRATION_MS = 3600000;
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET);
    private final Map<String, String> users = new ConcurrentHashMap<>();

    public AuthService() {
        users.put("admin", "qwerty123");
        users.put("user", "qwerty123");
    }

    public String generateToken(String username) {
        return JWT.create()
                .withSubject(username)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .sign(algorithm);
    }

    public String validateToken(String token) throws JWTVerificationException {
        DecodedJWT jwt = JWT.require(algorithm).build().verify(token);
        return jwt.getSubject();
    }

    public boolean authenticate(String login, String password) {
        String stored = users.get(login);
        return stored != null && stored.equals(password);
    }
}