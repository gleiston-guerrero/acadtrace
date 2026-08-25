package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.common.jdbc.GenericRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CalificacionConsultaService {

    private final NamedParameterJdbcTemplate jdbc;

    public CalificacionConsultaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarCursos(Long idAnoLectivo, Long idGrado) {
        String sql = """
                SELECT asig.id_asignacion,
                       asig.id_grado, g.nombre AS grado_nombre,
                       asig.id_paralelo, p.letra AS paralelo_letra,
                       asig.id_asignatura, a.nombre AS asignatura_nombre, a.codigo AS asignatura_codigo,
                       asig.id_docente,
                       TRIM(CONCAT(COALESCE(per.nombres, ''), ' ', COALESCE(per.apellidos, ''))) AS docente_nombre,
                       COALESCE(asig.horas_semanales, 4) AS horas_semanales,
                       COALESCE((
                           SELECT COUNT(*)
                           FROM sga_secretaria.matriculas m
                           WHERE m.id_grado = asig.id_grado
                             AND m.id_paralelo = asig.id_paralelo
                             AND m.id_ano_lectivo = asig.id_ano_lectivo
                             AND m.estado = 'ACTIVA'
                       ), 0) AS total_estudiantes
                FROM sga_principal.asignaciones asig
                JOIN sga_principal.grados g ON g.id_grado = asig.id_grado
                JOIN sga_principal.paralelos p ON p.id_paralelo = asig.id_paralelo
                JOIN sga_principal.asignaturas a ON a.id_asignatura = asig.id_asignatura
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = asig.id_docente
                LEFT JOIN sga_principal.personas per ON per.id_usuario = u.id_usuario
                WHERE asig.activo = true
                """ + (idAnoLectivo != null ? " AND asig.id_ano_lectivo = :idAno" : "")
                    + (idGrado != null ? " AND asig.id_grado = :idGrado" : "")
                    + " ORDER BY g.orden, p.letra, a.nombre";

        MapSqlParameterSource params = new MapSqlParameterSource();
        if (idAnoLectivo != null) params.addValue("idAno", idAnoLectivo);
        if (idGrado != null) params.addValue("idGrado", idGrado);

        return jdbc.query(sql, params, GenericRowMapper.INSTANCE);
    }

    public List<Map<String, Object>> listarPeriodos(Long idAnoLectivo) {
        String sql = """
                SELECT id_periodo, id_ano_lectivo, nombre, tipo, fecha_inicio, fecha_fin, activo
                FROM sga_docente.periodos_evaluacion
                """ + (idAnoLectivo != null ? " WHERE id_ano_lectivo = :idAno" : "")
                    + " ORDER BY fecha_inicio ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (idAnoLectivo != null) params.addValue("idAno", idAnoLectivo);
        return jdbc.query(sql, params, GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> obtenerMatrizNotas(long idAsignacion, Long idPeriodo) {
        String sqlAsig = """
                SELECT asig.id_asignacion, asig.id_grado, g.nombre AS grado_nombre,
                       asig.id_paralelo, p.letra AS paralelo_letra,
                       asig.id_asignatura, a.nombre AS asignatura_nombre, a.codigo AS asignatura_codigo,
                       asig.id_ano_lectivo,
                       TRIM(CONCAT(COALESCE(per.nombres, ''), ' ', COALESCE(per.apellidos, ''))) AS docente_nombre
                FROM sga_principal.asignaciones asig
                JOIN sga_principal.grados g ON g.id_grado = asig.id_grado
                JOIN sga_principal.paralelos p ON p.id_paralelo = asig.id_paralelo
                JOIN sga_principal.asignaturas a ON a.id_asignatura = asig.id_asignatura
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = asig.id_docente
                LEFT JOIN sga_principal.personas per ON per.id_usuario = u.id_usuario
                WHERE asig.id_asignacion = :idAsignacion
                """;
        List<Map<String, Object>> asigRows = jdbc.query(sqlAsig,
                new MapSqlParameterSource("idAsignacion", idAsignacion), GenericRowMapper.INSTANCE);
        if (asigRows.isEmpty()) {
            throw ApiException.notFound("Asignación no encontrada");
        }
        Map<String, Object> asignacion = asigRows.get(0);
        Long idGrado = ((Number) asignacion.get("id_grado")).longValue();
        Long idParalelo = ((Number) asignacion.get("id_paralelo")).longValue();
        Long idAnoLectivo = ((Number) asignacion.get("id_ano_lectivo")).longValue();

        String sqlEstudiantes = """
                SELECT m.id_matricula, m.numero_orden, m.estado,
                       e.id_estudiante, e.cedula, e.nombres, e.apellidos,
                       TRIM(CONCAT(e.apellidos, ' ', e.nombres)) AS estudiante_nombre
                FROM sga_secretaria.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE m.id_grado = :idGrado
                  AND m.id_paralelo = :idParalelo
                  AND m.id_ano_lectivo = :idAno
                  AND m.estado = 'ACTIVA'
                ORDER BY e.apellidos, e.nombres
                """;
        List<Map<String, Object>> estudiantes = jdbc.query(sqlEstudiantes,
                new MapSqlParameterSource()
                        .addValue("idGrado", idGrado)
                        .addValue("idParalelo", idParalelo)
                        .addValue("idAno", idAnoLectivo),
                GenericRowMapper.INSTANCE);

        List<Map<String, Object>> periodos = listarPeriodos(idAnoLectivo);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("asignacion", asignacion);
        response.put("periodos", periodos);
        response.put("estudiantes", estudiantes);
        return response;
    }
}