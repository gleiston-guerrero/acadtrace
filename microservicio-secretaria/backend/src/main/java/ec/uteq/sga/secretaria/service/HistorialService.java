package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.PromocionRequest;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HistorialService {

    private static final Logger log = LoggerFactory.getLogger(HistorialService.class);

    private final NamedParameterJdbcTemplate jdbc;
    private final LamportClock lamportClock;
    private final CatalogoService catalogo;

    public HistorialService(NamedParameterJdbcTemplate jdbc, LamportClock lamportClock, CatalogoService catalogo) {
        this.jdbc = jdbc;
        this.lamportClock = lamportClock;
        this.catalogo = catalogo;
    }

    /**
     * Arranca el reloj logico desde el mayor lamport_ts ya persistido, para no
     * repetir valores tras un reinicio del servicio. Si la columna todavia no
     * existe (falta correr db/migrations/002_lamport_clock.sql) arranca en 0
     * en vez de tumbar el boot del servicio.
     */
    @PostConstruct
    void seedLamportClock() {
        try {
            Long max = jdbc.getJdbcTemplate().queryForObject(
                    "SELECT COALESCE(MAX(lamport_ts), 0) FROM sga_principal.historial_promocion", Long.class);
            lamportClock.seed(max == null ? 0 : max);
        } catch (DataAccessException e) {
            log.warn("No se pudo leer lamport_ts (¿falta correr db/migrations/002_lamport_clock.sql?). " +
                    "El reloj logico arranca en 0.", e);
        }
    }

    public Map<String, Object> historialEstudiante(long idEstudiante) {
        List<Map<String, Object>> est = jdbc.query(
                "SELECT id_estudiante, nombres, apellidos, cedula, codigo_estudiante " +
                        "FROM sga_secretaria.estudiantes WHERE id_estudiante = :id",
                new MapSqlParameterSource("id", idEstudiante), GenericRowMapper.INSTANCE);
        if (est.isEmpty()) throw ApiException.notFound("Estudiante no encontrado");

        String sql = """
                SELECT hp.id_historial, hp.resultado, hp.promedio_anual, hp.observaciones,
                       hp.fecha_registro, hp.id_grado_origen, hp.id_ano_lectivo,
                       u.username AS registrado_por
                FROM sga_principal.historial_promocion hp
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = hp.registrado_por
                WHERE hp.id_estudiante = :id
                """;
        List<Map<String, Object>> historial = jdbc.query(
                sql, new MapSqlParameterSource("id", idEstudiante), GenericRowMapper.INSTANCE);
        enriquecerConCatalogo(historial);
        historial.sort(Comparator.comparing(
                (Map<String, Object> row) -> (java.time.LocalDate) row.get("fecha_inicio"),
                Comparator.nullsLast(Comparator.reverseOrder())));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("estudiante", est.get(0));
        result.put("historial", historial);
        return result;
    }

    @Transactional
    public Map<String, Object> registrarPromocion(PromocionRequest dto, String username) {
        List<Map<String, Object>> mat = jdbc.query(
                "SELECT m.id_matricula, m.id_estudiante, m.id_grado, m.id_ano_lectivo " +
                        "FROM sga_principal.matriculas m WHERE m.id_matricula = :id",
                new MapSqlParameterSource("id", dto.id_matricula()), GenericRowMapper.INSTANCE);
        if (mat.isEmpty()) throw ApiException.notFound("Matrícula no encontrada");
        Map<String, Object> m = mat.get(0);

        List<Long> dup = jdbc.query(
                "SELECT id_historial FROM sga_principal.historial_promocion WHERE id_matricula = :id",
                new MapSqlParameterSource("id", dto.id_matricula()), (rs, n) -> rs.getLong("id_historial"));
        if (!dup.isEmpty()) throw ApiException.conflict("Ya existe registro de promoción para esta matrícula");

        List<Long> userIds = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username",
                new MapSqlParameterSource("username", username), (rs, n) -> rs.getLong("id_usuario"));
        Long registradoPor = userIds.isEmpty() ? null : userIds.get(0);

        String observaciones = (dto.observaciones() == null || dto.observaciones().isBlank()) ? null : dto.observaciones();

        // "resultado" nunca aparece con cast explicito en el SQL Node original (a diferencia de
        // genero_t/estado_matricula_t), lo que indica que es una columna varchar/text simple, no un enum custom.
        // lamport_ts: orden causal del evento, ver LamportClock e independiente de fecha_registro (reloj de pared).
        long lamportTs = lamportClock.tick();
        jdbc.update("""
                INSERT INTO sga_principal.historial_promocion
                  (id_matricula, id_estudiante, id_grado_origen, id_ano_lectivo,
                   resultado, promedio_anual, observaciones, registrado_por, lamport_ts)
                VALUES (:idMatricula, :idEstudiante, :idGrado, :idAno, :resultado, :promedio, :observaciones, :registradoPor, :lamportTs)
                """,
                new MapSqlParameterSource()
                        .addValue("lamportTs", lamportTs)
                        .addValue("idMatricula", dto.id_matricula())
                        .addValue("idEstudiante", m.get("id_estudiante"))
                        .addValue("idGrado", m.get("id_grado"))
                        .addValue("idAno", m.get("id_ano_lectivo"))
                        .addValue("resultado", dto.resultado())
                        .addValue("promedio", dto.promedio_anual())
                        .addValue("observaciones", observaciones)
                        .addValue("registradoPor", registradoPor));

        String estadoMatricula = "PROMOVIDO".equals(dto.resultado()) ? "PROMOVIDA" : "NO_PROMOVIDA";
        jdbc.update(
                "UPDATE sga_principal.matriculas SET estado = :estado::sga_principal.estado_matricula_t " +
                        "WHERE id_matricula = :id",
                new MapSqlParameterSource().addValue("estado", estadoMatricula).addValue("id", dto.id_matricula()));

        Number idEstudiante = (Number) m.get("id_estudiante");
        return historialEstudiante(idEstudiante.longValue());
    }

    public List<Map<String, Object>> resumenPromocion(long idAnoLectivo) {
        List<Map<String, Object>> conteos = jdbc.query("""
                SELECT id_grado_origen,
                       COUNT(*) FILTER (WHERE resultado = 'PROMOVIDO') AS promovidos,
                       COUNT(*) FILTER (WHERE resultado = 'NO_PROMOVIDO') AS no_promovidos,
                       COUNT(*) FILTER (WHERE resultado = 'RETIRADO') AS retirados,
                       ROUND(AVG(promedio_anual)::numeric, 2) AS promedio_general,
                       COUNT(id_historial) AS total_registrados
                FROM sga_principal.historial_promocion
                WHERE id_ano_lectivo = :idAno
                GROUP BY id_grado_origen
                """, new MapSqlParameterSource("idAno", idAnoLectivo), GenericRowMapper.INSTANCE);

        Map<Long, Map<String, Object>> porGrado = new LinkedHashMap<>();
        for (Map<String, Object> c : conteos) {
            porGrado.put(((Number) c.get("id_grado_origen")).longValue(), c);
        }

        return catalogo.grados().stream()
                .filter(CatalogoService.Grado::activo)
                .sorted(Comparator.comparingInt(CatalogoService.Grado::orden))
                .filter(g -> porGrado.containsKey(g.id()))
                .map(g -> {
                    Map<String, Object> c = porGrado.get(g.id());
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("grado", g.nombre());
                    row.put("promovidos", c.get("promovidos"));
                    row.put("no_promovidos", c.get("no_promovidos"));
                    row.put("retirados", c.get("retirados"));
                    row.put("promedio_general", c.get("promedio_general"));
                    row.put("total_registrados", c.get("total_registrados"));
                    return row;
                })
                .toList();
    }

    public List<Map<String, Object>> estudiantesSinPromocion(long idAnoLectivo) {
        String sql = """
                SELECT m.id_matricula, e.id_estudiante,
                       e.nombres || ' ' || e.apellidos AS estudiante,
                       e.cedula, m.id_grado, m.id_paralelo
                FROM sga_principal.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE m.id_ano_lectivo = :idAno
                  AND NOT EXISTS (
                    SELECT 1 FROM sga_principal.historial_promocion hp
                    WHERE hp.id_matricula = m.id_matricula
                  )
                """;
        List<Map<String, Object>> data = jdbc.query(sql, new MapSqlParameterSource("idAno", idAnoLectivo), GenericRowMapper.INSTANCE);

        Map<Long, CatalogoService.Grado> grados = new LinkedHashMap<>();
        catalogo.grados().forEach(g -> grados.put(g.id(), g));
        Map<Long, CatalogoService.Paralelo> paralelos = new LinkedHashMap<>();
        catalogo.paralelos(null).forEach(p -> paralelos.put(p.id(), p));

        for (Map<String, Object> row : data) {
            Long idGrado = row.get("id_grado") instanceof Number n ? n.longValue() : null;
            Long idParalelo = row.get("id_paralelo") instanceof Number n ? n.longValue() : null;
            CatalogoService.Grado grado = idGrado != null ? grados.get(idGrado) : null;
            CatalogoService.Paralelo paralelo = idParalelo != null ? paralelos.get(idParalelo) : null;
            row.put("grado", grado != null ? grado.nombre() : null);
            row.put("paralelo", paralelo != null ? paralelo.letra() : null);
        }

        Comparator<Map<String, Object>> porOrdenGrado = Comparator.comparing(row -> {
            Long idGrado = row.get("id_grado") instanceof Number n ? n.longValue() : null;
            CatalogoService.Grado grado = idGrado != null ? grados.get(idGrado) : null;
            return grado != null ? grado.orden() : Integer.MAX_VALUE;
        });
        data.sort(porOrdenGrado
                .thenComparing(row -> (String) row.get("paralelo"), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(row -> (String) row.get("estudiante"), Comparator.nullsLast(Comparator.naturalOrder())));

        return data;
    }

    /** Agrega grado/ano_lectivo (nombre + fechas) a filas de historial_promocion via el catalogo gRPC. */
    private void enriquecerConCatalogo(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return;
        Map<Long, CatalogoService.Grado> grados = new LinkedHashMap<>();
        catalogo.grados().forEach(g -> grados.put(g.id(), g));
        Map<Long, CatalogoService.AnoLectivo> anos = new LinkedHashMap<>();
        catalogo.anosLectivos().forEach(a -> anos.put(a.id(), a));

        for (Map<String, Object> fila : filas) {
            Long idGrado = fila.get("id_grado_origen") instanceof Number n ? n.longValue() : null;
            Long idAno = fila.get("id_ano_lectivo") instanceof Number n ? n.longValue() : null;
            CatalogoService.Grado grado = idGrado != null ? grados.get(idGrado) : null;
            CatalogoService.AnoLectivo ano = idAno != null ? anos.get(idAno) : null;

            fila.put("grado", grado != null ? grado.nombre() : null);
            fila.put("ano_lectivo", ano != null ? ano.nombre() : null);
            fila.put("fecha_inicio", ano != null ? ano.fechaInicio() : null);
            fila.put("fecha_fin", ano != null ? ano.fechaFin() : null);
        }
    }
}
