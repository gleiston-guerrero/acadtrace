package ec.uteq.sga.secretaria.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Pruebas Unitarias: JwtService (Microservicio Secretaría)")
class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test-only-jwt-secret";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret);
    }

    @Test
    @DisplayName("Generar token JWT y parsearlo correctamente")
    void test_generateAndParseToken_success() {
        String token = jwtService.generateToken("ernesto.luna", List.of("SECRETARIA", "ADMIN"));
        assertThat(token).isNotBlank();

        AuthenticatedUser user = jwtService.parse(token);
        assertThat(user).isNotNull();
        assertThat(user.username()).isEqualTo("ernesto.luna");
        assertThat(user.roles()).containsExactly("SECRETARIA", "ADMIN");
    }

    @Test
    @DisplayName("Rechazar token con firma alterada")
    void test_parseInvalidSignature_throwsException() {
        String token = jwtService.generateToken("ernesto.luna", List.of("SECRETARIA"));
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertThatThrownBy(() -> jwtService.parse(tamperedToken))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("Parsear token sin roles genera lista vacía")
    void test_parseTokenWithoutRoles() {
        String token = jwtService.generateToken("user.noroles", List.of());
        AuthenticatedUser user = jwtService.parse(token);

        assertThat(user.username()).isEqualTo("user.noroles");
        assertThat(user.roles()).isEmpty();
    }
}
