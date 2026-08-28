package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.security.JwtService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/auth")
public class AuthController {

    private final NamedParameterJdbcTemplate jdbc;
    private final JwtService jwtService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthController(NamedParameterJdbcTemplate jdbc, JwtService jwtService) {
        this.jdbc = jdbc;
        this.jwtService = jwtService;
    }

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.username().isBlank() ||
            request.password() == null || request.password().isBlank()) {
            throw ApiException.badRequest("Usuario y contraseña son requeridos");
        }

        String sqlUsuario = """
                SELECT u.id_usuario, u.username, u.password_hash, u.estado, u.primer_ingreso
                FROM sga_principal.usuarios u
                WHERE u.username = :username
                """;

        List<Map<String, Object>> usuarios = jdbc.queryForList(sqlUsuario, new MapSqlParameterSource("username", request.username().trim()));
        if (usuarios.isEmpty()) {
            throw ApiException.unauthorized("Usuario o contraseña incorrectos");
        }

        Map<String, Object> usuario = usuarios.get(0);
        boolean estado = Boolean.TRUE.equals(usuario.get("estado"));
        if (!estado) {
            throw ApiException.unauthorized("Tu usuario se encuentra inactivo");
        }

        String hash = (String) usuario.get("password_hash");
        if (hash == null || !passwordEncoder.matches(request.password(), hash)) {
            throw ApiException.unauthorized("Usuario o contraseña incorrectos");
        }

        long idUsuario = ((Number) usuario.get("id_usuario")).longValue();
        String username = (String) usuario.get("username");
        boolean primerIngreso = Boolean.TRUE.equals(usuario.get("primer_ingreso"));

        String sqlRoles = """
                SELECT r.nombre
                FROM sga_principal.roles r
                JOIN sga_principal.usuario_roles ur ON ur.id_rol = r.id_rol
                WHERE ur.id_usuario = :idUsuario
                """;

        List<String> roles = jdbc.queryForList(sqlRoles, new MapSqlParameterSource("idUsuario", idUsuario), String.class);
        if (roles.isEmpty()) {
            throw ApiException.forbidden("Tu usuario no tiene un rol asignado");
        }

        String token = jwtService.generateToken(username, roles);

        return Map.of(
                "token", token,
                "idUsuario", idUsuario,
                "username", username,
                "roles", roles,
                "primerIngreso", primerIngreso
        );
    }
}