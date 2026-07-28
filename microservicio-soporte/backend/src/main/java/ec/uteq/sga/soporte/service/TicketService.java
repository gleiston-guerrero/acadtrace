package ec.uteq.sga.soporte.service;

import ec.uteq.sga.soporte.common.ApiException;
import ec.uteq.sga.soporte.common.jdbc.GenericRowMapper;
import ec.uteq.sga.soporte.dto.ActualizarTicketRequest;
import ec.uteq.sga.soporte.dto.ComentarioRequest;
import ec.uteq.sga.soporte.dto.EscalarTicketRequest;
import ec.uteq.sga.soporte.dto.TicketRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica de negocio del modulo de soporte tecnico. Toda la data vive en el
 * esquema propio sga_soporte (tablas: tickets, comentarios, historial_ticket);
 * ningun otro servicio lee esta base directamente.
 *
 * Las consultas devuelven las columnas con alias camelCase para que el
 * frontend las consuma directamente (idTicket, numeroTicket, creadoPor, ...).
 */
@Service
public class TicketService {

    private static final Set<String> ESTADOS = Set.of("ABIERTO", "EN_PROCESO", "RESUELTO", "CERRADO");
    private static final Set<String> PRIORIDADES = Set.of("BAJO", "MEDIO", "ALTO", "CRITICO");
    private static final Set<String> CATEGORIAS = Set.of("HARDWARE", "SOFTWARE", "RED", "CUENTA", "OTRO");

    private static final String SELECT_TICKET = """
            SELECT id_ticket        AS "idTicket",
                   numero_ticket    AS "numeroTicket",
                   titulo, descripcion, categoria, prioridad, estado,
                   creado_por       AS "creadoPor",
                   asignado_a       AS "asignadoA",
                   solucion_aplicada AS "solucionAplicada",
                   fecha_creacion   AS "fechaCreacion",
                   fecha_resolucion AS "fechaResolucion"
            FROM sga_soporte.tickets
            """;

    private static final String SELECT_COMENTARIO = """
            SELECT id_comentario  AS "idComentario",
                   id_ticket      AS "idTicket",
                   autor, contenido,
                   nota_interna   AS "notaInterna",
                   fecha_creacion AS "fechaCreacion"
            FROM sga_soporte.comentarios
            """;

    private static final String SELECT_HISTORIAL = """
            SELECT id_historial       AS "idHistorial",
                   id_ticket          AS "idTicket",
                   campo,
                   valor_anterior     AS "valorAnterior",
                   valor_nuevo        AS "valorNuevo",
                   modificado_por     AS "modificadoPor",
                   fecha_modificacion AS "fechaModificacion"
            FROM sga_soporte.historial_ticket
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public TicketService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Map<String, Object>> listar() {
        return jdbc.query(SELECT_TICKET + " ORDER BY fecha_creacion DESC", GenericRowMapper.INSTANCE);
    }

    public List<Map<String, Object>> misTickets(String username) {
        return jdbc.query(SELECT_TICKET + " WHERE creado_por = :u ORDER BY fecha_creacion DESC",
                new MapSqlParameterSource("u", username), GenericRowMapper.INSTANCE);
    }

    public Map<String, Object> estadisticas() {
        return jdbc.queryForObject("""
                SELECT COUNT(*)                                        AS total,
                       COUNT(*) FILTER (WHERE estado = 'ABIERTO')     AS abiertos,
                       COUNT(*) FILTER (WHERE estado = 'EN_PROCESO')  AS "enProceso",
                       COUNT(*) FILTER (WHERE estado = 'RESUELTO')    AS resueltos,
                       COUNT(*) FILTER (WHERE estado = 'CERRADO')     AS cerrados
                FROM sga_soporte.tickets
                """, new MapSqlParameterSource(), GenericRowMapper.INSTANCE);
    }

    /**
     * Reportes de gestion: totales y tiempo promedio de resolucion (horas)
     * agrupados por categoria y por tecnico asignado, mas un resumen general.
     * El tiempo de resolucion solo se calcula sobre tickets que ya tienen
     * fecha_resolucion (RESUELTO o CERRADO).
     */
    public Map<String, Object> reportes() {
        List<Map<String, Object>> porCategoria = jdbc.query("""
                SELECT categoria,
                       COUNT(*)                                                    AS total,
                       COUNT(*) FILTER (WHERE estado IN ('RESUELTO','CERRADO'))    AS resueltos,
                       ROUND(CAST(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion)) / 3600.0)
                             FILTER (WHERE fecha_resolucion IS NOT NULL) AS numeric), 1) AS "tiempoPromedioHoras"
                FROM sga_soporte.tickets
                GROUP BY categoria
                ORDER BY total DESC
                """, new MapSqlParameterSource(), GenericRowMapper.INSTANCE);

        List<Map<String, Object>> porTecnico = jdbc.query("""
                SELECT asignado_a                                                  AS tecnico,
                       COUNT(*)                                                    AS total,
                       COUNT(*) FILTER (WHERE estado IN ('RESUELTO','CERRADO'))    AS resueltos,
                       ROUND(CAST(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion)) / 3600.0)
                             FILTER (WHERE fecha_resolucion IS NOT NULL) AS numeric), 1) AS "tiempoPromedioHoras"
                FROM sga_soporte.tickets
                WHERE asignado_a IS NOT NULL
                GROUP BY asignado_a
                ORDER BY total DESC
                """, new MapSqlParameterSource(), GenericRowMapper.INSTANCE);

        Map<String, Object> general = jdbc.queryForObject("""
                SELECT COUNT(*)                                                     AS "totalTickets",
                       COUNT(*) FILTER (WHERE fecha_resolucion IS NOT NULL)         AS "ticketsResueltos",
                       ROUND(CAST(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion)) / 3600.0)
                             FILTER (WHERE fecha_resolucion IS NOT NULL) AS numeric), 1) AS "tiempoPromedioHoras"
                FROM sga_soporte.tickets
                """, new MapSqlParameterSource(), GenericRowMapper.INSTANCE);

        return Map.of(
                "porCategoria", porCategoria,
                "porTecnico", porTecnico,
                "general", general
        );
    }

    public Map<String, Object> obtener(long id) {
        List<Map<String, Object>> rows = jdbc.query(SELECT_TICKET + " WHERE id_ticket = :id",
                new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) {
            throw ApiException.notFound("Ticket no encontrado");
        }
        return rows.get(0);
    }

    @Transactional
    public Map<String, Object> crear(TicketRequest req, String creadoPor) {
        String prioridad = req.prioridad().toUpperCase();
        if (!PRIORIDADES.contains(prioridad)) {
            throw ApiException.badRequest("Prioridad inválida (BAJO, MEDIO, ALTO, CRITICO)");
        }
        String categoria = req.categoria().toUpperCase();
        if (!CATEGORIAS.contains(categoria)) {
            throw ApiException.badRequest("Categoría inválida (HARDWARE, SOFTWARE, RED, CUENTA, OTRO)");
        }

        String numero = "TK-" + System.currentTimeMillis();

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("numero", numero)
                .addValue("titulo", req.titulo())
                .addValue("descripcion", req.descripcion())
                .addValue("categoria", categoria)
                .addValue("prioridad", prioridad)
                .addValue("creado_por", creadoPor);

        Long id = jdbc.queryForObject(
                "INSERT INTO sga_soporte.tickets "
                        + "(numero_ticket, titulo, descripcion, categoria, prioridad, estado, creado_por, fecha_creacion) "
                        + "VALUES (:numero, :titulo, :descripcion, :categoria, :prioridad, 'ABIERTO', :creado_por, NOW()) "
                        + "RETURNING id_ticket",
                params, Long.class);

        return obtener(id == null ? 0 : id);
    }

    @Transactional
    public Map<String, Object> actualizar(long id, ActualizarTicketRequest req, String modificadoPor) {
        String estado = req.estado().toUpperCase();
        if (!ESTADOS.contains(estado)) {
            throw ApiException.badRequest("Estado inválido (ABIERTO, EN_PROCESO, RESUELTO, CERRADO)");
        }

        Map<String, Object> actual = obtener(id); // valida existencia y trae valores previos
        String estadoAnterior = (String) actual.get("estado");
        String asignadoAnterior = (String) actual.get("asignadoA");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("estado", estado)
                .addValue("asignado", req.asignadoA())
                .addValue("solucion", req.solucionAplicada());
        jdbc.update(
                "UPDATE sga_soporte.tickets SET estado = :estado, "
                        + "asignado_a = COALESCE(:asignado, asignado_a), "
                        + "solucion_aplicada = COALESCE(:solucion, solucion_aplicada), "
                        + "fecha_resolucion = CASE WHEN :estado IN ('RESUELTO','CERRADO') THEN NOW() ELSE fecha_resolucion END "
                        + "WHERE id_ticket = :id",
                params);

        if (!estado.equals(estadoAnterior)) {
            registrarHistorial(id, "ESTADO", estadoAnterior, estado, modificadoPor);
        }
        if (req.asignadoA() != null && !req.asignadoA().equals(asignadoAnterior)) {
            registrarHistorial(id, "ASIGNADO_A", asignadoAnterior, req.asignadoA(), modificadoPor);
        }

        return obtener(id);
    }

    /**
     * Escalamiento: sube la prioridad y/o reasigna el ticket a un tecnico
     * superior. Requiere al menos uno de los dos cambios y siempre deja el
     * motivo registrado como nota interna en el timeline del ticket.
     */
    @Transactional
    public Map<String, Object> escalar(long id, EscalarTicketRequest req, String actor) {
        Map<String, Object> actual = obtener(id); // valida existencia y trae valores previos
        String estadoActual = (String) actual.get("estado");
        if ("CERRADO".equals(estadoActual)) {
            throw ApiException.badRequest("No se puede escalar un ticket cerrado");
        }

        String nuevaPrioridad = req.nuevaPrioridad() == null || req.nuevaPrioridad().isBlank()
                ? null : req.nuevaPrioridad().toUpperCase();
        if (nuevaPrioridad != null && !PRIORIDADES.contains(nuevaPrioridad)) {
            throw ApiException.badRequest("Prioridad inválida (BAJO, MEDIO, ALTO, CRITICO)");
        }
        String nuevoAsignado = req.nuevoAsignado() == null || req.nuevoAsignado().isBlank()
                ? null : req.nuevoAsignado();

        if (nuevaPrioridad == null && nuevoAsignado == null) {
            throw ApiException.badRequest("El escalamiento debe cambiar la prioridad y/o reasignar el ticket");
        }

        String prioridadAnterior = (String) actual.get("prioridad");
        String asignadoAnterior = (String) actual.get("asignadoA");

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("prioridad", nuevaPrioridad)
                .addValue("asignado", nuevoAsignado);
        jdbc.update(
                "UPDATE sga_soporte.tickets SET "
                        + "prioridad = COALESCE(:prioridad, prioridad), "
                        + "asignado_a = COALESCE(:asignado, asignado_a) "
                        + "WHERE id_ticket = :id",
                params);

        if (nuevaPrioridad != null && !nuevaPrioridad.equals(prioridadAnterior)) {
            registrarHistorial(id, "PRIORIDAD", prioridadAnterior, nuevaPrioridad, actor);
        }
        if (nuevoAsignado != null && !nuevoAsignado.equals(asignadoAnterior)) {
            registrarHistorial(id, "ASIGNADO_A", asignadoAnterior, nuevoAsignado, actor);
        }

        MapSqlParameterSource comentario = new MapSqlParameterSource()
                .addValue("id_ticket", id)
                .addValue("autor", actor)
                .addValue("contenido", "Escalamiento: " + req.motivo())
                .addValue("nota_interna", true);
        jdbc.update(
                "INSERT INTO sga_soporte.comentarios (id_ticket, autor, contenido, nota_interna, fecha_creacion) "
                        + "VALUES (:id_ticket, :autor, :contenido, :nota_interna, NOW())",
                comentario);

        return obtener(id);
    }

    private void registrarHistorial(long idTicket, String campo, String anterior, String nuevo, String modificadoPor) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id_ticket", idTicket)
                .addValue("campo", campo)
                .addValue("anterior", anterior)
                .addValue("nuevo", nuevo)
                .addValue("modificado_por", modificadoPor);
        jdbc.update(
                "INSERT INTO sga_soporte.historial_ticket "
                        + "(id_ticket, campo, valor_anterior, valor_nuevo, modificado_por, fecha_modificacion) "
                        + "VALUES (:id_ticket, :campo, :anterior, :nuevo, :modificado_por, NOW())",
                params);
    }

    public List<Map<String, Object>> listarHistorial(long idTicket) {
        obtener(idTicket);
        return jdbc.query(SELECT_HISTORIAL + " WHERE id_ticket = :id ORDER BY fecha_modificacion ASC",
                new MapSqlParameterSource("id", idTicket), GenericRowMapper.INSTANCE);
    }

    public List<Map<String, Object>> listarComentarios(long idTicket) {
        obtener(idTicket);
        return jdbc.query(SELECT_COMENTARIO + " WHERE id_ticket = :id ORDER BY fecha_creacion ASC",
                new MapSqlParameterSource("id", idTicket), GenericRowMapper.INSTANCE);
    }

    @Transactional
    public Map<String, Object> comentar(long idTicket, ComentarioRequest req, String autor) {
        obtener(idTicket);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id_ticket", idTicket)
                .addValue("autor", autor)
                .addValue("contenido", req.contenido())
                .addValue("nota_interna", req.notaInterna() != null && req.notaInterna());
        Long id = jdbc.queryForObject(
                "INSERT INTO sga_soporte.comentarios (id_ticket, autor, contenido, nota_interna, fecha_creacion) "
                        + "VALUES (:id_ticket, :autor, :contenido, :nota_interna, NOW()) RETURNING id_comentario",
                params, Long.class);

        return jdbc.query(SELECT_COMENTARIO + " WHERE id_comentario = :id",
                new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE).get(0);
    }
}