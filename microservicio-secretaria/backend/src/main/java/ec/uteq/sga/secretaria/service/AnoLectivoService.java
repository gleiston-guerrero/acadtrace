package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.AnoLectivoRequest;
import ec.uteq.sga.secretaria.dto.PeriodoEvaluacionRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class AnoLectivoService {

    private final NamedParameterJdbcTemplate jdbc;

    public AnoLectivoService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listarTodos() {
        String sql = """
                SELECT a.id_ano_lectivo, a.nombre, a.fecha_inicio, a.fecha_fin, a.es_actual, a.fecha_creacion,
                       COALESCE((
                           SELECT COUNT(*) FROM sga_secretaria.matriculas m
                           WHERE m.id_ano_lectivo = a.id_ano_lectivo AND m.estado = 'ACTIVA'
                       ), 0) AS total_matriculados,
                       COALESCE((
                           SELECT COUNT(*) FROM sga_docente.periodos_evaluacion p
                           WHERE p.id_ano_lectivo = a.id_ano_lectivo
                       ), 0) AS total_periodos
                FROM sga_principal.anos_lectivos a
                ORDER BY a.fecha_inicio DESC
                """;
        return jdbc.query(sql, GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> obtenerActual() {
        String sql = """
                SELECT a.id_ano_lectivo, a.nombre, a.fecha_inicio, a.fecha_fin, a.es_actual, a.fecha_creacion
                FROM sga_principal.anos_lectivos a
                WHERE a.es_actual = true
                LIMIT 1
                """;
        List<Map<String, Object>> rows = jdbc.query(sql, GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("No hay un año lectivo marcado como actual");
        }
        return rows.get(0);
    }

    public Map<String, Object> obtenerPorId(long id) {
        String sql = """
                SELECT a.id_ano_lectivo, a.nombre, a.fecha_inicio, a.fecha_fin, a.es_actual, a.fecha_creacion
                FROM sga_principal.anos_lectivos a
                WHERE a.id_ano_lectivo = :id
                """;
        List<Map<String, Object>> rows = jdbc.query(sql, new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("Año lectivo no encontrado");
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> crear(AnoLectivoRequest dto) {
        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw ApiException.badRequest("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        String sqlDup = "SELECT COUNT(*) FROM sga_principal.anos_lectivos WHERE LOWER(nombre) = LOWER(:nombre)";
        Integer count = jdbc.queryForObject(sqlDup, new MapSqlParameterSource("nombre", dto.nombre().trim()), Integer.class);
        if (count != null && count > 0) {
            throw ApiException.conflict("Ya existe un año lectivo con ese nombre");
        }

        String sqlInsert = """
                INSERT INTO sga_principal.anos_lectivos (nombre, fecha_inicio, fecha_fin, es_actual, fecha_creacion)
                VALUES (:nombre, :fechaInicio, :fechaFin, false, NOW())
                RETURNING id_ano_lectivo
                """;
        Long id = jdbc.queryForObject(sqlInsert,
                new MapSqlParameterSource()
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("fechaInicio", dto.fechaInicio())
                        .addValue("fechaFin", dto.fechaFin()),
                Long.class);

        // Crear por defecto los 3 trimestres en sga_docente.periodos_evaluacion
        crearPeriodosPorDefecto(id, dto);

        return obtenerPorId(id);
    }

    private void crearPeriodosPorDefecto(long idAno, AnoLectivoRequest dto) {
        long dias = java.time.temporal.ChronoUnit.DAYS.between(dto.fechaInicio(), dto.fechaFin());
        long tercio = dias / 3;

        java.time.LocalDate t1Fin = dto.fechaInicio().plusDays(tercio);
        java.time.LocalDate t2Inicio = t1Fin.plusDays(1);
        java.time.LocalDate t2Fin = t2Inicio.plusDays(tercio);
        java.time.LocalDate t3Inicio = t2Fin.plusDays(1);

        String insertPeriodo = """
                INSERT INTO sga_docente.periodos_evaluacion (id_ano_lectivo, tipo_periodo, nombre, fecha_inicio, fecha_fin, activo, orden)
                VALUES (:idAno, :tipo, :nombre, :fechaInicio, :fechaFin, true, :orden)
                """;

        jdbc.update(insertPeriodo, new MapSqlParameterSource()
                .addValue("idAno", idAno).addValue("tipo", "PRIMER_TRIMESTRE").addValue("nombre", "Primer Trimestre")
                .addValue("fechaInicio", dto.fechaInicio()).addValue("fechaFin", t1Fin).addValue("orden", 1));

        jdbc.update(insertPeriodo, new MapSqlParameterSource()
                .addValue("idAno", idAno).addValue("tipo", "SEGUNDO_TRIMESTRE").addValue("nombre", "Segundo Trimestre")
                .addValue("fechaInicio", t2Inicio).addValue("fechaFin", t2Fin).addValue("orden", 2));

        jdbc.update(insertPeriodo, new MapSqlParameterSource()
                .addValue("idAno", idAno).addValue("tipo", "TERCER_TRIMESTRE").addValue("nombre", "Tercer Trimestre")
                .addValue("fechaInicio", t3Inicio).addValue("fechaFin", dto.fechaFin()).addValue("orden", 3));
    }

    @Transactional
    public Map<String, Object> actualizar(long id, AnoLectivoRequest dto) {
        obtenerPorId(id);

        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw ApiException.badRequest("La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        String sqlDup = "SELECT COUNT(*) FROM sga_principal.anos_lectivos WHERE LOWER(nombre) = LOWER(:nombre) AND id_ano_lectivo != :id";
        Integer count = jdbc.queryForObject(sqlDup,
                new MapSqlParameterSource().addValue("nombre", dto.nombre().trim()).addValue("id", id), Integer.class);
        if (count != null && count > 0) {
            throw ApiException.conflict("Ya existe otro año lectivo con ese nombre");
        }

        String sqlUpdate = """
                UPDATE sga_principal.anos_lectivos
                SET nombre = :nombre,
                    fecha_inicio = :fechaInicio,
                    fecha_fin = :fechaFin
                WHERE id_ano_lectivo = :id
                """;
        jdbc.update(sqlUpdate,
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("nombre", dto.nombre().trim())
                        .addValue("fechaInicio", dto.fechaInicio())
                        .addValue("fechaFin", dto.fechaFin()));

        return obtenerPorId(id);
    }

    @Transactional
    public void establecerActual(long id) {
        obtenerPorId(id);
        jdbc.update("UPDATE sga_principal.anos_lectivos SET es_actual = false", new MapSqlParameterSource());
        jdbc.update("UPDATE sga_principal.anos_lectivos SET es_actual = true WHERE id_ano_lectivo = :id",
                new MapSqlParameterSource("id", id));
    }

    public List<Map<String, Object>> listarPeriodos(long idAno) {
        String sql = """
                SELECT id_periodo, id_ano_lectivo, tipo_periodo, nombre, fecha_inicio, fecha_fin, activo, orden
                FROM sga_docente.periodos_evaluacion
                WHERE id_ano_lectivo = :idAno
                ORDER BY orden ASC, fecha_inicio ASC
                """;
        return jdbc.query(sql, new MapSqlParameterSource("idAno", idAno), GenericRowMapper.INSTANCE);
    }

    @Transactional
    public Map<String, Object> crearPeriodo(long idAno, PeriodoEvaluacionRequest dto) {
        obtenerPorId(idAno);
        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw ApiException.badRequest("La fecha fin del periodo no puede ser anterior a la fecha de inicio");
        }

        String insert = """
                INSERT INTO sga_docente.periodos_evaluacion (id_ano_lectivo, tipo_periodo, nombre, fecha_inicio, fecha_fin, activo, orden)
                VALUES (:idAno, :tipo, :nombre, :fechaInicio, :fechaFin, true, :orden)
                RETURNING id_periodo
                """;
        Long idPeriodo = jdbc.queryForObject(insert,
                new MapSqlParameterSource()
                        .addValue("idAno", idAno)
                        .addValue("tipo", dto.tipo() != null ? dto.tipo() : "TRIMESTRE")
                        .addValue("nombre", dto.nombre())
                        .addValue("fechaInicio", dto.fechaInicio())
                        .addValue("fechaFin", dto.fechaFin())
                        .addValue("orden", dto.orden() != null ? dto.orden() : 1),
                Long.class);

        return Map.of("id_periodo", idPeriodo, "nombre", dto.nombre(), "mensaje", "Periodo creado exitosamente");
    }

    @Transactional
    public void actualizarPeriodo(long idPeriodo, PeriodoEvaluacionRequest dto) {
        if (dto.fechaFin().isBefore(dto.fechaInicio())) {
            throw ApiException.badRequest("La fecha fin del periodo no puede ser anterior a la fecha de inicio");
        }

        int updated = jdbc.update("""
                UPDATE sga_docente.periodos_evaluacion
                SET nombre = :nombre,
                    fecha_inicio = :fechaInicio,
                    fecha_fin = :fechaFin,
                    orden = COALESCE(:orden, orden)
                WHERE id_periodo = :id
                """,
                new MapSqlParameterSource()
                        .addValue("id", idPeriodo)
                        .addValue("nombre", dto.nombre())
                        .addValue("fechaInicio", dto.fechaInicio())
                        .addValue("fechaFin", dto.fechaFin())
                        .addValue("orden", dto.orden()));

        if (updated == 0) {
            throw ApiException.notFound("Periodo de evaluación no encontrado");
        }
    }

    @Transactional
    public void eliminarPeriodo(long idPeriodo) {
        int deleted = jdbc.update("DELETE FROM sga_docente.periodos_evaluacion WHERE id_periodo = :id",
                new MapSqlParameterSource("id", idPeriodo));
        if (deleted == 0) {
            throw ApiException.notFound("Periodo de evaluación no encontrado");
        }
    }
}