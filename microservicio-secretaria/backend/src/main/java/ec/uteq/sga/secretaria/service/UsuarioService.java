package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.UsuarioRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UsuarioService {

    private final NamedParameterJdbcTemplate jdbc;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    public UsuarioService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarTodos() {
        String sql = """
                SELECT
                  u.id_usuario,
                  u.username,
                  u.correo,
                  u.estado,
                  u.primer_ingreso,
                  u.ultimo_acceso,
                  COALESCE(p.nombres, '')   AS nombres,
                  COALESCE(p.apellidos, '') AS apellidos,
                  COALESCE(p.cedula, '')    AS cedula,
                  COALESCE(p.telefono, '')  AS telefono,
                  COALESCE(p.cargo, '')     AS cargo,
                  COALESCE(p.titulo_academico, '') AS titulo_academico,
                  COALESCE(p.especializacion, '')  AS especializacion,
                  COALESCE(
                    (SELECT array_agg(r.nombre ORDER BY r.nombre)
                     FROM sga_principal.usuario_roles ur
                     JOIN sga_principal.roles r ON r.id_rol = ur.id_rol
                     WHERE ur.id_usuario = u.id_usuario),
                    ARRAY[]::varchar[]
                  ) AS roles
                FROM sga_principal.usuarios u
                LEFT JOIN sga_principal.personas p ON p.id_usuario = u.id_usuario
                WHERE u.estado = true
                ORDER BY COALESCE(p.apellidos, u.username), COALESCE(p.nombres, '')
                """;
        return jdbc.query(sql, GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> obtenerPorId(long id) {
        String sql = """
                SELECT
                  u.id_usuario, u.username, u.correo, u.estado,
                  u.primer_ingreso, u.ultimo_acceso, u.fecha_creacion,
                  COALESCE(p.nombres, '')   AS nombres,
                  COALESCE(p.apellidos, '') AS apellidos,
                  COALESCE(p.cedula, '')    AS cedula,
                  COALESCE(p.telefono, '')  AS telefono,
                  COALESCE(p.cargo, '')     AS cargo,
                  COALESCE(p.titulo_academico, '') AS titulo_academico,
                  COALESCE(p.especializacion, '')  AS especializacion,
                  COALESCE(
                    (SELECT array_agg(r.nombre ORDER BY r.nombre)
                     FROM sga_principal.usuario_roles ur
                     JOIN sga_principal.roles r ON r.id_rol = ur.id_rol
                     WHERE ur.id_usuario = u.id_usuario),
                    ARRAY[]::varchar[]
                  ) AS roles
                FROM sga_principal.usuarios u
                LEFT JOIN sga_principal.personas p ON p.id_usuario = u.id_usuario
                WHERE u.id_usuario = :id
                """;
        List<Map<String, Object>> rows = jdbc.query(sql, new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("Usuario no encontrado");
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> crear(UsuarioRequest dto) {
        List<Long> dup = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username OR correo = :correo",
                new MapSqlParameterSource().addValue("username", dto.username()).addValue("correo", dto.correo()),
                (rs, n) -> rs.getLong("id_usuario"));
        if (!dup.isEmpty()) throw ApiException.conflict("Username o correo ya existe");

        String primerNombre = dto.nombres().trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
        String tempPass = primerNombre + Year.now().getValue();
        String hash = passwordEncoder.encode(tempPass);

        Long idUsuario = jdbc.queryForObject(
                "INSERT INTO sga_principal.usuarios (username, correo, password_hash, estado, primer_ingreso) " +
                        "VALUES (:username, :correo, :hash, true, true) RETURNING id_usuario",
                new MapSqlParameterSource()
                        .addValue("username", dto.username())
                        .addValue("correo", dto.correo())
                        .addValue("hash", hash),
                Long.class);

        if (dto.nombres() != null && !dto.nombres().isBlank() && dto.apellidos() != null && !dto.apellidos().isBlank()) {
            jdbc.update(
                    "INSERT INTO sga_principal.personas " +
                            "(id_usuario, cedula, nombres, apellidos, telefono, cargo, titulo_academico, especializacion, correo_personal) " +
                            "VALUES (:idUsuario, :cedula, :nombres, :apellidos, :telefono, :cargo, :tituloAcademico, :especializacion, :correo)",
                    new MapSqlParameterSource()
                            .addValue("idUsuario", idUsuario)
                            .addValue("cedula", blankToNull(dto.cedula()))
                            .addValue("nombres", dto.nombres())
                            .addValue("apellidos", dto.apellidos())
                            .addValue("telefono", blankToNull(dto.telefono()))
                            .addValue("cargo", blankToNull(dto.cargo()))
                            .addValue("tituloAcademico", blankToNull(dto.titulo_academico()))
                            .addValue("especializacion", blankToNull(dto.especializacion()))
                            .addValue("correo", blankToNull(dto.correo())));
        }

        if (dto.roles() != null && !dto.roles().isEmpty()) {
            for (String rolNombre : dto.roles()) {
                List<Long> rolIds = jdbc.query(
                        "SELECT id_rol FROM sga_principal.roles WHERE nombre = :nombre AND activo = true",
                        new MapSqlParameterSource("nombre", rolNombre), (rs, n) -> rs.getLong("id_rol"));
                if (!rolIds.isEmpty()) {
                    jdbc.update(
                            "INSERT INTO sga_principal.usuario_roles (id_usuario, id_rol) VALUES (:idUsuario, :idRol) " +
                                    "ON CONFLICT DO NOTHING",
                            new MapSqlParameterSource().addValue("idUsuario", idUsuario).addValue("idRol", rolIds.get(0)));
                }
            }
        }

        Map<String, Object> created = new LinkedHashMap<>(obtenerPorId(idUsuario));
        created.put("temp_password", tempPass);
        return created;
    }

    public Map<String, Object> resetearPassword(long id) {
        Map<String, Object> usuario = obtenerPorId(id);
        String nombres = (String) usuario.get("nombres");
        String nombre = (nombres != null && !nombres.isBlank())
                ? nombres.trim().split("\\s+")[0]
                : (String) usuario.get("username");
        String tempPass = nombre.toLowerCase(Locale.ROOT) + Year.now().getValue();
        String hash = passwordEncoder.encode(tempPass);

        jdbc.update(
                "UPDATE sga_principal.usuarios SET " +
                        "password_hash = :hash, primer_ingreso = true, " +
                        "intentos_fallidos = 0, bloqueado_hasta = NULL, fecha_actualizacion = NOW() " +
                        "WHERE id_usuario = :id",
                new MapSqlParameterSource().addValue("hash", hash).addValue("id", id));

        return Map.of("temp_password", tempPass);
    }

    public void cambiarEstado(long id, boolean estado) {
        obtenerPorId(id);
        jdbc.update(
                "UPDATE sga_principal.usuarios SET estado = :estado, fecha_actualizacion = NOW() WHERE id_usuario = :id",
                new MapSqlParameterSource().addValue("estado", estado).addValue("id", id));
    }

    public List<Map<String, Object>> listarRoles() {
        return jdbc.query(
                "SELECT id_rol, nombre, descripcion FROM sga_principal.roles WHERE activo = true ORDER BY nombre",
                GenericRowMapper.INSTANCE);
    }

    @Transactional
    public void asignarRoles(long id, List<String> roles) {
        obtenerPorId(id);
        jdbc.update("DELETE FROM sga_principal.usuario_roles WHERE id_usuario = :id",
                new MapSqlParameterSource("id", id));
        for (String rolNombre : roles) {
            List<Long> rolIds = jdbc.query(
                    "SELECT id_rol FROM sga_principal.roles WHERE nombre = :nombre",
                    new MapSqlParameterSource("nombre", rolNombre), (rs, n) -> rs.getLong("id_rol"));
            if (!rolIds.isEmpty()) {
                jdbc.update(
                        "INSERT INTO sga_principal.usuario_roles (id_usuario, id_rol) VALUES (:id, :idRol)",
                        new MapSqlParameterSource().addValue("id", id).addValue("idRol", rolIds.get(0)));
            }
        }
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
