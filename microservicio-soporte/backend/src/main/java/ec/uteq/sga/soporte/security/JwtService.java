package ec.uteq.sga.soporte.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Valida los JWT emitidos por sga-principal (mismo JWT_SECRET compartido, claims sub + roles).
 */
@Component
public class JwtService {

    private final JwtParser parser;

    public JwtService(@Value("${app.jwt.secret}") String secret) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        // El parser construido es thread-safe; cada peticion sigue verificando el JWT.
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = parser
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();
        List<?> rawRoles = claims.get("roles", List.class);
        List<String> roles = rawRoles == null
                ? List.of()
                : rawRoles.stream().map(String::valueOf).toList();

        return new AuthenticatedUser(username, roles);
    }
}
