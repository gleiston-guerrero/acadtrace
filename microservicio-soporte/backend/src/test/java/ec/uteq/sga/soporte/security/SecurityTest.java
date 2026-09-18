package ec.uteq.sga.soporte.security;

import ec.uteq.sga.soporte.common.ApiException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Pruebas Unitarias: Seguridad, JWT y Argument Resolver")
class SecurityTest {

    private static final String SECRET = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";  // gitleaks:allow

    @Test
    @DisplayName("1. AuthenticatedUser -- Roles y permisos jerárquicos")
    void authenticatedUser_rolesAndPermissions() {
        AuthenticatedUser userDirector = new AuthenticatedUser("dir", List.of("DIRECTOR"));
        assertThat(userDirector.isDirector()).isTrue();
        assertThat(userDirector.isTecnicoOrDirector()).isTrue();

        AuthenticatedUser userTecnico = new AuthenticatedUser("tec", List.of("SOPORTE_TECNICO"));
        assertThat(userTecnico.isDirector()).isFalse();
        assertThat(userTecnico.isTecnicoOrDirector()).isTrue();

        AuthenticatedUser userAdmin = new AuthenticatedUser("adm", List.of("ADMINISTRADOR"));
        assertThat(userAdmin.isTecnicoOrDirector()).isTrue();

        AuthenticatedUser userDocente = new AuthenticatedUser("doc", List.of("DOCENTE"));
        assertThat(userDocente.isDirector()).isFalse();
        assertThat(userDocente.isTecnicoOrDirector()).isFalse();

        AuthenticatedUser userVacio = new AuthenticatedUser("nullUser", null);
        assertThat(userVacio.isDirector()).isFalse();
        assertThat(userVacio.isTecnicoOrDirector()).isFalse();
    }

    @Test
    @DisplayName("2. JwtService -- Parseo exitoso de token con roles")
    void jwtService_parseValidToken() {
        JwtService jwtService = new JwtService(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("carlos.perez")
                .claim("roles", List.of("SOPORTE_TECNICO", "DOCENTE"))
                .signWith(key)
                .compact();

        AuthenticatedUser user = jwtService.parse(token);

        assertThat(user).isNotNull();
        assertThat(user.username()).isEqualTo("carlos.perez");
        assertThat(user.roles()).containsExactly("SOPORTE_TECNICO", "DOCENTE");
    }

    @Test
    @DisplayName("3. JwtService -- Token sin claim de roles asigna lista vacía")
    void jwtService_parseTokenWithoutRoles() {
        JwtService jwtService = new JwtService(SECRET);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

        String token = Jwts.builder()
                .subject("usuario.simple")
                .signWith(key)
                .compact();

        AuthenticatedUser user = jwtService.parse(token);

        assertThat(user.username()).isEqualTo("usuario.simple");
        assertThat(user.roles()).isEmpty();
    }

    @Test
    @DisplayName("4. CurrentUserArgumentResolver -- Inyección y manejo de token ausente")
    void argumentResolver_supportsAndResolves() {
        CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

        MethodParameter validParam = mock(MethodParameter.class);
        given(validParam.getParameterType()).willAnswer(inv -> AuthenticatedUser.class);
        assertThat(resolver.supportsParameter(validParam)).isTrue();

        MethodParameter invalidParam = mock(MethodParameter.class);
        given(invalidParam.getParameterType()).willAnswer(inv -> String.class);
        assertThat(resolver.supportsParameter(invalidParam)).isFalse();

        NativeWebRequest requestWithUser = mock(NativeWebRequest.class);
        AuthenticatedUser mockUser = new AuthenticatedUser("testUser", List.of("DOCENTE"));
        given(requestWithUser.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST))
                .willReturn(mockUser);

        Object resolved = resolver.resolveArgument(validParam, null, requestWithUser, null);
        assertThat(resolved).isEqualTo(mockUser);

        NativeWebRequest requestWithoutUser = mock(NativeWebRequest.class);
        given(requestWithoutUser.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST))
                .willReturn(null);

        assertThatThrownBy(() -> resolver.resolveArgument(validParam, null, requestWithoutUser, null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Token no proporcionado");
    }
}
