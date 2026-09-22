package ec.uteq.sga.soporte.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtParserReuseTest {
    @Test
    void sharedParserKeepsClaimsIsolatedAndChecksEverySignatureAndExpiration() throws Exception {
        var key = Jwts.SIG.HS256.key().build();
        // JwtService recibe bytes UTF-8; clave efimera ASCII solo para esta prueba.
        String secret = java.util.HexFormat.of().formatHex(key.getEncoded());
        var signingKey = io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        var service = new JwtService(secret);
        var executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Void>> tasks = IntStream.range(0, 64).mapToObj(i -> (Callable<Void>) () -> {
                String subject = "synthetic-user-" + i;
                String token = Jwts.builder().subject(subject).claim("roles", List.of("DIRECTOR"))
                        .signWith(signingKey).compact();
                var user = service.parse(token);
                assertThat(user.username()).isEqualTo(subject);
                assertThat(user.roles()).containsExactly("DIRECTOR");
                return null;
            }).toList();
            for (var result : executor.invokeAll(tasks)) result.get();
        } finally {
            executor.shutdownNow();
        }
        String wrongSignature = Jwts.builder().subject("invalid").signWith(key).compact();
        assertThatThrownBy(() -> service.parse(wrongSignature)).isInstanceOf(SignatureException.class);
        String expired = Jwts.builder().subject("expired")
                .expiration(Date.from(Instant.now().minusSeconds(60))).signWith(signingKey).compact();
        assertThatThrownBy(() -> service.parse(expired)).isInstanceOf(ExpiredJwtException.class);
    }
}
