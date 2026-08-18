package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AsistenciaConsultaService {

    private final NamedParameterJdbcTemplate jdbc;

    public AsistenciaConsultaService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Lectura directa de asistencias por asignacion, mismo esquema que consulta sga_docente
     * ya usado en el reporte de asistencia mensual (ReportesService.asistenciaMensual). */
    public List<Map<String, Object>> porAsignacion(long idAsignacion) {
        return jdbc.query("""
                SELECT id_asistencia, id_matricula, id_asignacion, id_periodo, fecha, estado, justificacion
                FROM sga_docente.asistencias
                WHERE id_asignacion = :idAsignacion
                ORDER BY fecha
                """,
                new MapSqlParameterSource("idAsignacion", idAsignacion),
                GenericRowMapper.INSTANCE);
    }
}
