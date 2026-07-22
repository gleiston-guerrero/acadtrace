package ec.uteq.sga.soporte.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Autentica + autoriza en un solo paso para /api/soporte/*.
 *
 * Los nombres de rol son los que emite sga-principal en el JWT (tabla
 * sga_principal.roles: DIRECTOR, SECRETARIA, DOCENTE, SOPORTE_TECNICO), sin
 * prefijo "ROLE_". Solo SOPORTE_TECNICO y DIRECTOR entran a este modulo.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> REQUIRED_ROLES = Set.of("SOPORTE_TECNICO", "DIRECTOR");

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            writeError(response, 401, "Token no proporcionado");
            return;
        }

        String token = authHeader.substring(7);
        AuthenticatedUser user;
        try {
            user = jwtService.parse(token);
        } catch (JwtException | IllegalArgumentException e) {
            writeError(response, 401, "Token invalido o expirado");
            return;
        }

        boolean authorized = user.roles() != null && user.roles().stream().anyMatch(REQUIRED_ROLES::contains);
        if (!authorized) {
            writeError(response, 403, "Acceso denegado");
            return;
        }

        request.setAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE, user);
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
