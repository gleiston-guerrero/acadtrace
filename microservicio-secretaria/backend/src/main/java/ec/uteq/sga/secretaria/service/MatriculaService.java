package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.PageResult;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.MatriculaRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatriculaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CatalogoService catalogo;

    public MatriculaService(NamedParameterJdbcTemplate jdbc, CatalogoService catalogo) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
    }

    public List<Map<String, Object>> paralelosPorGrado(long idGrado) {
        return catalogo.paralelos(idGrado).stream()
                .filter(CatalogoService.Paralelo::activo)
                .sorted(Comparator.comparing(CatalogoService.Paralelo::letra))
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id_paralelo", p.id());
                    row.put("letra", p.letra());
                    return row;
                })
                .toList();
    }

    public PageResult<Map<String, Object>> listarPorAnoLectivo(long idAnoLectivo, int page, int limit, String search) {
        int offset = (page - 1) * limit;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("idAno", idAnoLectivo);
        String where = "WHERE m.id_ano_lectivo = :idAno";
        if (search != null && !search.isBlank()) {
            params.addValue("like", "%" + search + "%");
            where += " AND (e.nombres ILIKE :like OR e.apellidos ILIKE :like OR e.cedula ILIKE :like)";
        }

        Long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM sga_principal.matriculas m " +
                        "JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante " + where,
                params, Long.class);

        params.addValue("limit", limit).addValue("offset", offset);
        // Orden por id_grado/id_paralelo (no por g.orden/p.letra: el catalogo se
        // consume por gRPC, no por JOIN SQL, ver CatalogoService) - suficiente
        // porque grados y paralelos normalmente se crean en el mismo orden en
        // que se muestran.
        String sql = """
                SELECT m.id_matricula, m.numero_orden, m.fecha_registro, m.estado, m.observaciones,
                       m.id_grado, m.id_paralelo, m.id_ano_lectivo,
                       e.id_estudiante, e.cedula, e.codigo_estudiante,
                       e.nombres || ' ' || e.apellidos AS estudiante,
                       u.username AS registrado_por
                FROM sga_principal.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = m.registrado_por
                %s
                ORDER BY m.id_grado, m.id_paralelo, e.apellidos
                LIMIT :limit OFFSET :offset
                """.formatted(where);
        List<Map<String, Object>> data = jdbc.query(sql, params, GenericRowMapper.INSTANCE);
        enriquecerConCatalogo(data);

        return PageResult.of(data, total == null ? 0 : total, page, limit);
    }

    public List<Map<String, Object>> estadisticasPorGrado(long idAnoLectivo) {
        List<Map<String, Object>> conteos = jdbc.query("""
                SELECT id_grado, id_paralelo,
                       COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE estado = 'ACTIVA') AS activas,
                       COUNT(*) FILTER (WHERE estado = 'RETIRADA') AS retiradas
                FROM sga_principal.matriculas
                WHERE id_ano_lectivo = :idAno
                GROUP BY id_grado, id_paralelo
                """, new MapSqlParameterSource("idAno", idAnoLectivo), GenericRowMapper.INSTANCE);

        Map<String, Map<String, Object>> conteoPorClave = new LinkedHashMap<>();
        for (Map<String, Object> c : conteos) {
            conteoPorClave.put(c.get("id_grado") + ":" + c.get("id_paralelo"), c);
        }

        List<CatalogoService.Grado> grados = catalogo.grados().stream()
                .filter(CatalogoService.Grado::activo)
                .sorted(Comparator.comparingInt(CatalogoService.Grado::orden))
                .toList();

        List<Map<String, Object>> resultado = new java.util.ArrayList<>();
        for (CatalogoService.Grado grado : grados) {
            List<CatalogoService.Paralelo> paralelos = catalogo.paralelos(grado.id()).stream()
                    .filter(CatalogoService.Paralelo::activo)
                    .sorted(Comparator.comparing(CatalogoService.Paralelo::letra))
                    .toList();
            for (CatalogoService.Paralelo paralelo : paralelos) {
                Map<String, Object> conteo = conteoPorClave.get(grado.id() + ":" + paralelo.id());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("grado", grado.nombre());
                row.put("paralelo", paralelo.letra());
                row.put("total", conteo != null ? conteo.get("total") : 0L);
                row.put("activas", conteo != null ? conteo.get("activas") : 0L);
                row.put("retiradas", conteo != null ? conteo.get("retiradas") : 0L);
                resultado.add(row);
            }
        }
        return resultado;
    }

    public List<Map<String, Object>> listarPorEstudiante(long idEstudiante) {
        String sql = """
                SELECT m.id_matricula, m.numero_orden, m.fecha_registro, m.estado,
                       m.id_grado, m.id_paralelo, m.id_ano_lectivo,
                       hp.resultado AS resultado_promocion, hp.promedio_anual
                FROM sga_principal.matriculas m
                LEFT JOIN sga_principal.historial_promocion hp ON hp.id_matricula = m.id_matricula
                WHERE m.id_estudiante = :id
                """;
        List<Map<String, Object>> data = jdbc.query(sql, new MapSqlParameterSource("id", idEstudiante), GenericRowMapper.INSTANCE);
        enriquecerConCatalogo(data);
        data.sort(Comparator.comparing(
                (Map<String, Object> row) -> (java.time.LocalDate) row.get("ano_lectivo_fecha_inicio"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return data;
    }

    public Map<String, Object> obtenerPorId(long id) {
        String sql = """
                SELECT m.*,
                       e.nombres || ' ' || e.apellidos AS estudiante,
                       e.cedula, e.codigo_estudiante,
                       u.username AS registrado_por
                FROM sga_principal.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = m.registrado_por
                WHERE m.id_matricula = :id
                """;
        List<Map<String, Object>> rows = jdbc.query(sql, new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("Matrícula no encontrada");
        List<Map<String, Object>> data = new java.util.ArrayList<>(rows);
        enriquecerConCatalogo(data);
        return data.get(0);
    }

    public Map<String, Object> crear(MatriculaRequest dto, String username) {
        MapSqlParameterSource dupParams = new MapSqlParameterSource()
                .addValue("idEstudiante", dto.id_estudiante())
                .addValue("idAno", dto.id_ano_lectivo());
        List<Long> dup = jdbc.query(
                "SELECT id_matricula FROM sga_principal.matriculas WHERE id_estudiante = :idEstudiante AND id_ano_lectivo = :idAno",
                dupParams, (rs, n) -> rs.getLong("id_matricula"));
        if (!dup.isEmpty()) throw ApiException.conflict("El estudiante ya tiene matrícula en ese año lectivo");

        List<Long> userIds = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username",
                new MapSqlParameterSource("username", username), (rs, n) -> rs.getLong("id_usuario"));
        Long registradoPor = userIds.isEmpty() ? null : userIds.get(0);

        Integer maxOrden = jdbc.queryForObject(
                "SELECT COALESCE(MAX(numero_orden), 0) FROM sga_principal.matriculas WHERE id_ano_lectivo = :idAno",
                new MapSqlParameterSource("idAno", dto.id_ano_lectivo()), Integer.class);
        int numeroOrden = (maxOrden == null ? 0 : maxOrden) + 1;

        String estado = (dto.estado() == null || dto.estado().isBlank()) ? "ACTIVA" : dto.estado();
        String observaciones = (dto.observaciones() == null || dto.observaciones().isBlank()) ? null : dto.observaciones();

        MapSqlParameterSource insertParams = new MapSqlParameterSource()
                .addValue("idEstudiante", dto.id_estudiante())
                .addValue("idGrado", dto.id_grado())
                .addValue("idParalelo", dto.id_paralelo())
                .addValue("idAno", dto.id_ano_lectivo())
                .addValue("numeroOrden", numeroOrden)
                .addValue("estado", estado)
                .addValue("observaciones", observaciones)
                .addValue("registradoPor", registradoPor);

        // ::sga_principal.estado_matricula_t agregado por estrictez de PgJDBC (ver nota en EstudianteService)
        String insertSql = """
                INSERT INTO sga_principal.matriculas
                  (id_estudiante, id_grado, id_paralelo, id_ano_lectivo,
                   numero_orden, fecha_registro, estado, observaciones, registrado_por)
                VALUES (:idEstudiante, :idGrado, :idParalelo, :idAno, :numeroOrden, CURRENT_DATE,
                        :estado::sga_principal.estado_matricula_t, :observaciones, :registradoPor)
                RETURNING id_matricula
                """;
        Long newId = jdbc.queryForObject(insertSql, insertParams, Long.class);
        return obtenerPorId(newId);
    }

    public void cambiarEstado(long id, String estado) {
        obtenerPorId(id);
        jdbc.update(
                "UPDATE sga_principal.matriculas SET estado = :estado::sga_principal.estado_matricula_t WHERE id_matricula = :id",
                new MapSqlParameterSource().addValue("estado", estado).addValue("id", id));
    }

    /**
     * Agrega grado/paralelo/ano_lectivo (nombre + letra) a cada fila usando el
     * catalogo de sga-principal por gRPC en vez de un JOIN SQL directo, para
     * no acoplar Secretaria a las tablas de catalogo de otro servicio.
     */
    private void enriquecerConCatalogo(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return;
        Map<Long, CatalogoService.Grado> grados = new LinkedHashMap<>();
        catalogo.grados().forEach(g -> grados.put(g.id(), g));
        Map<Long, CatalogoService.Paralelo> paralelos = new LinkedHashMap<>();
        catalogo.paralelos(null).forEach(p -> paralelos.put(p.id(), p));
        Map<Long, CatalogoService.AnoLectivo> anos = new LinkedHashMap<>();
        catalogo.anosLectivos().forEach(a -> anos.put(a.id(), a));

        for (Map<String, Object> fila : filas) {
            Long idGrado = toLong(fila.get("id_grado"));
            Long idParalelo = toLong(fila.get("id_paralelo"));
            Long idAno = toLong(fila.get("id_ano_lectivo"));

            CatalogoService.Grado grado = idGrado != null ? grados.get(idGrado) : null;
            CatalogoService.Paralelo paralelo = idParalelo != null ? paralelos.get(idParalelo) : null;
            CatalogoService.AnoLectivo ano = idAno != null ? anos.get(idAno) : null;

            fila.put("grado", grado != null ? grado.nombre() : null);
            fila.put("paralelo", paralelo != null ? paralelo.letra() : null);
            fila.put("ano_lectivo", ano != null ? ano.nombre() : null);
            fila.put("ano_lectivo_fecha_inicio", ano != null ? ano.fechaInicio() : null);
            // Alias sin prefijo: listarPorEstudiante devolvia fecha_inicio/fecha_fin
            // "pelados" (columnas de anos_lectivos sin alias) antes del JOIN por gRPC.
            fila.put("fecha_inicio", ano != null ? ano.fechaInicio() : null);
            fila.put("fecha_fin", ano != null ? ano.fechaFin() : null);
        }
    }

    private static Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
