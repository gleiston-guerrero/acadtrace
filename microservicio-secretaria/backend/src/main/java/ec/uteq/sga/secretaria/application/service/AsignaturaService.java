package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.domain.dto.AsignaturaRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AsignaturaService {

    private final NamedParameterJdbcTemplate jdbc;

    public AsignaturaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarTodos() {
        String sql = """
                SELECT id_asignatura, nombre, codigo, descripcion, horas_semana, activa, fecha_creacion
                FROM sga_principal.asignaturas
                ORDER BY nombre ASC
                """;
        return jdbc.query(sql, GenericRowMapper.INSTANCE);
    }

    public List<Map<String, Object>> listarActivas() {
        String sql = """
                SELECT id_asignatura, nombre, codigo, descripcion, horas_semana, activa, fecha_creacion
                FROM sga_principal.asignaturas
                WHERE activa = true
                ORDER BY nombre ASC
                """;
        return jdbc.query(sql, GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> obtenerPorId(long id) {
        String sql = """
                SELECT id_asignatura, nombre, codigo, descripcion, horas_semana, activa, fecha_creacion
                FROM sga_principal.asignaturas
                WHERE id_asignatura = :id
                """;
        List<Map<String, Object>> rows = jdbc.query(sql, new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("Asignatura no encontrada");
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> crear(AsignaturaRequest dto) {
        String sqlDup = """
                SELECT id_asignatura FROM sga_principal.asignaturas
                WHERE LOWER(nombre) = LOWER(:nombre)
                   OR (codigo IS NOT NULL AND :codigo IS NOT NULL AND codigo != '' AND LOWER(codigo) = LOWER(:codigo))
                """;
        List<Long> dup = jdbc.query(sqlDup,
                new MapSqlParameterSource()
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("codigo", blankToNull(dto.codigo())),
                (rs, n) -> rs.getLong("id_asignatura"));

        if (!dup.isEmpty()) {
            throw ApiException.conflict("Ya existe una asignatura con ese nombre o código");
        }

        String sqlInsert = """
                INSERT INTO sga_principal.asignaturas (nombre, codigo, descripcion, horas_semana, activa, fecha_creacion)
                VALUES (:nombre, :codigo, :descripcion, :horasSemana, true, NOW())
                RETURNING id_asignatura
                """;
        Long id = jdbc.queryForObject(sqlInsert,
                new MapSqlParameterSource()
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("codigo", blankToNull(dto.codigo()))
                        .addValue("descripcion", blankToNull(dto.descripcion()))
                        .addValue("horasSemana", dto.horasSemanales() != null ? dto.horasSemanales() : 4),
                Long.class);

        return obtenerPorId(id);
    }

    @Transactional
    public Map<String, Object> actualizar(long id, AsignaturaRequest dto) {
        obtenerPorId(id);

        String sqlDup = """
                SELECT id_asignatura FROM sga_principal.asignaturas
                WHERE id_asignatura != :id
                  AND (LOWER(nombre) = LOWER(:nombre)
                       OR (codigo IS NOT NULL AND :codigo IS NOT NULL AND codigo != '' AND LOWER(codigo) = LOWER(:codigo)))
                """;
        List<Long> dup = jdbc.query(sqlDup,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("codigo", blankToNull(dto.codigo())),
                (rs, n) -> rs.getLong("id_asignatura"));

        if (!dup.isEmpty()) {
            throw ApiException.conflict("Ya existe otra asignatura con ese nombre o código");
        }

        String sqlUpdate = """
                UPDATE sga_principal.asignaturas
                SET nombre = :nombre,
                    codigo = :codigo,
                    descripcion = :descripcion,
                    horas_semana = :horasSemana
                WHERE id_asignatura = :id
                """;
        jdbc.update(sqlUpdate,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("codigo", blankToNull(dto.codigo()))
                        .addValue("descripcion", blankToNull(dto.descripcion()))
                        .addValue("horasSemana", dto.horasSemanales() != null ? dto.horasSemanales() : 4));

        return obtenerPorId(id);
    }

    @Transactional
    public void cambiarEstado(long id, boolean activo) {
        obtenerPorId(id);
        jdbc.update("UPDATE sga_principal.asignaturas SET activa = :activo WHERE id_asignatura = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("activo", activo));
    }

    private static String blankToNull(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }
}
