package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.FeriadosEcuador;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.EventoAcademicoRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Agrega en una sola respuesta las distintas fuentes de "cosas que pasan este mes":
 * eventos_academicos propios (sga_principal, CRUD de este controller), periodos de
 * evaluación y fechas de entrega de actividades (sga_docente, SQL directo igual que
 * las notas/asistencias de ReportesService), feriados de Ecuador (calculados, no hay
 * tabla para esto en ningún repo) y el año lectivo actual como referencia.
 */
@Service
public class CalendarioService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CatalogoService catalogo;

    public CalendarioService(NamedParameterJdbcTemplate jdbc, CatalogoService catalogo) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
    }

    public List<Map<String, Object>> calendario(String mes) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("El parámetro 'mes' debe tener formato YYYY-MM");
        }
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("inicio", inicio).addValue("fin", fin);

        List<Map<String, Object>> eventos = new ArrayList<>();

        eventos.addAll(jdbc.query("""
                SELECT id_evento, titulo, descripcion, fecha_inicio, fecha_fin, tipo
                FROM sga_principal.eventos_academicos
                WHERE fecha_inicio <= :fin AND COALESCE(fecha_fin, fecha_inicio) >= :inicio
                """, params, GenericRowMapper.INSTANCE));

        eventos.addAll(jdbc.query("""
                SELECT id_periodo, nombre AS titulo, fecha_inicio, fecha_fin, 'PERIODO_EVALUACION' AS tipo
                FROM sga_docente.periodos_evaluacion
                WHERE fecha_inicio <= :fin AND fecha_fin >= :inicio
                """, params, GenericRowMapper.INSTANCE));

        eventos.addAll(jdbc.query("""
                SELECT a.id_actividad, a.nombre AS titulo, a.fecha_entrega AS fecha_inicio,
                       NULL::date AS fecha_fin, 'ACTIVIDAD' AS tipo, asig.nombre AS materia
                FROM sga_docente.actividades a
                LEFT JOIN sga_principal.asignaciones asgn ON asgn.id_asignacion = a.id_asignacion
                LEFT JOIN sga_principal.asignaturas asig ON asig.id_asignatura = asgn.id_asignatura
                WHERE a.fecha_entrega BETWEEN :inicio AND :fin
                """, params, GenericRowMapper.INSTANCE));

        FeriadosEcuador.delAno(inicio.getYear()).stream()
                .filter(f -> !((LocalDate) f.get("fecha_inicio")).isBefore(inicio) && !((LocalDate) f.get("fecha_inicio")).isAfter(fin))
                .forEach(eventos::add);
        if (fin.getYear() != inicio.getYear()) {
            FeriadosEcuador.delAno(fin.getYear()).stream()
                    .filter(f -> !((LocalDate) f.get("fecha_inicio")).isBefore(inicio) && !((LocalDate) f.get("fecha_inicio")).isAfter(fin))
                    .forEach(eventos::add);
        }

        catalogo.anosLectivos().stream()
                .filter(CatalogoService.AnoLectivo::esActual)
                .filter(a -> !a.fechaInicio().isAfter(fin) && !a.fechaFin().isBefore(inicio))
                .findFirst()
                .ifPresent(a -> eventos.add(Map.of(
                        "titulo", "Año lectivo " + a.nombre(),
                        "fecha_inicio", a.fechaInicio(),
                        "fecha_fin", a.fechaFin(),
                        "tipo", "ANO_LECTIVO")));

        eventos.sort(Comparator.comparing(e -> (LocalDate) e.get("fecha_inicio")));
        return eventos;
    }

    public List<Map<String, Object>> listarEventos() {
        return jdbc.query("""
                SELECT id_evento, titulo, descripcion, fecha_inicio, fecha_fin, tipo, id_grado
                FROM sga_principal.eventos_academicos
                ORDER BY fecha_inicio DESC
                """, GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> crearEvento(EventoAcademicoRequest dto, String username) {
        Long idUsuario = resolverIdUsuario(username);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("titulo", dto.titulo())
                .addValue("descripcion", dto.descripcion())
                .addValue("fechaInicio", dto.fecha_inicio())
                .addValue("fechaFin", dto.fecha_fin())
                .addValue("tipo", dto.tipo())
                .addValue("idGrado", dto.id_grado())
                .addValue("creadoPor", idUsuario);
        String sql = """
                INSERT INTO sga_principal.eventos_academicos
                  (titulo, descripcion, fecha_inicio, fecha_fin, tipo, id_grado, creado_por)
                VALUES (:titulo, :descripcion, :fechaInicio, :fechaFin, :tipo, :idGrado, :creadoPor)
                RETURNING *
                """;
        return jdbc.query(sql, params, GenericRowMapper.INSTANCE).get(0);
    }

    public Map<String, Object> actualizarEvento(long id, EventoAcademicoRequest dto) {
        obtenerEvento(id);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("titulo", dto.titulo())
                .addValue("descripcion", dto.descripcion())
                .addValue("fechaInicio", dto.fecha_inicio())
                .addValue("fechaFin", dto.fecha_fin())
                .addValue("tipo", dto.tipo())
                .addValue("idGrado", dto.id_grado())
                .addValue("id", id);
        String sql = """
                UPDATE sga_principal.eventos_academicos SET
                  titulo = :titulo, descripcion = :descripcion, fecha_inicio = :fechaInicio,
                  fecha_fin = :fechaFin, tipo = :tipo, id_grado = :idGrado
                WHERE id_evento = :id
                RETURNING *
                """;
        return jdbc.query(sql, params, GenericRowMapper.INSTANCE).get(0);
    }

    public void eliminarEvento(long id) {
        obtenerEvento(id);
        jdbc.update("DELETE FROM sga_principal.eventos_academicos WHERE id_evento = :id",
                new MapSqlParameterSource("id", id));
    }

    private Long resolverIdUsuario(String username) {
        List<Long> ids = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username",
                new MapSqlParameterSource("username", username), (rs, n) -> rs.getLong("id_usuario"));
        return ids.isEmpty() ? null : ids.get(0);
    }

    private Map<String, Object> obtenerEvento(long id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM sga_principal.eventos_academicos WHERE id_evento = :id",
                new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("Evento académico no encontrado");
        return rows.get(0);
    }
}
