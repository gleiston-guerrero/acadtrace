package ec.uteq.sga.soporte.infrastructure.persistence;

import ec.uteq.sga.soporte.domain.port.TicketRepositoryPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.*;

@Repository
public class JdbcTicketRepository implements TicketRepositoryPort {

    private final JdbcTemplate jdbc;

    public JdbcTicketRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_BASE =
        "SELECT id_ticket AS \"id\", numero_ticket AS \"numeroTicket\", titulo, descripcion, " +
        "categoria, prioridad, estado, creado_por AS \"creadoPor\", asignado_a AS \"asignadoA\", " +
        "solucion_aplicada AS \"solucionAplicada\", fecha_creacion AS \"fechaCreacion\", " +
        "fecha_resolucion AS \"fechaResolucion\" FROM sga_soporte.tickets ";

    @Override
    public List<Map<String, Object>> listarTodos() {
        return jdbc.queryForList(SELECT_BASE + "ORDER BY id_ticket DESC");
    }

    @Override
    public List<Map<String, Object>> listarPorCreador(String username) {
        return jdbc.queryForList(SELECT_BASE + "WHERE creado_por = ? ORDER BY id_ticket DESC", username);
    }

    @Override
    public Optional<Map<String, Object>> buscarPorId(long id) {
        List<Map<String, Object>> res = jdbc.queryForList(SELECT_BASE + "WHERE id_ticket = ?", id);
        return res.isEmpty() ? Optional.empty() : Optional.of(res.get(0));
    }

    @Override
    public Map<String, Object> estadisticas() {
        String sql = "SELECT count(*) AS total, " +
                "count(*) FILTER (WHERE estado = 'ABIERTO') AS abiertos, " +
                "count(*) FILTER (WHERE estado = 'EN_PROCESO') AS en_proceso, " +
                "count(*) FILTER (WHERE estado = 'RESUELTO') AS resueltos, " +
                "count(*) FILTER (WHERE estado = 'CERRADO') AS cerrados " +
                "FROM sga_soporte.tickets";
        Map<String, Object> row = jdbc.queryForMap(sql);
        Map<String, Object> result = new HashMap<>();
        result.put("total", row.get("total"));
        result.put("abiertos", row.get("abiertos"));
        result.put("enProceso", row.get("en_proceso"));
        result.put("resueltos", row.get("resueltos"));
        result.put("cerrados", row.get("cerrados"));
        return result;
    }

    @Override
    public List<Map<String, Object>> reportePorCategoria() {
        String sql = "SELECT categoria, count(*) AS total, " +
                "ROUND(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion))/3600.0)::numeric, 2) AS tiempo_promedio_horas " +
                "FROM sga_soporte.tickets GROUP BY categoria";
        return jdbc.queryForList(sql);
    }

    @Override
    public List<Map<String, Object>> reportePorTecnico() {
        String sql = "SELECT COALESCE(asignado_a, 'Sin Asignar') AS tecnico, count(*) AS total, " +
                "ROUND(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion))/3600.0)::numeric, 2) AS tiempo_promedio_horas " +
                "FROM sga_soporte.tickets GROUP BY asignado_a";
        return jdbc.queryForList(sql);
    }

    @Override
    public Map<String, Object> reporteGeneral() {
        String sql = "SELECT count(*) AS total_tickets, " +
                "ROUND(AVG(EXTRACT(EPOCH FROM (fecha_resolucion - fecha_creacion))/3600.0)::numeric, 2) AS tiempo_promedio_resolucion_global " +
                "FROM sga_soporte.tickets WHERE fecha_resolucion IS NOT NULL";
        List<Map<String, Object>> list = jdbc.queryForList(sql);
        return list.isEmpty() ? Map.of("totalTickets", 0, "tiempoPromedioGlobal", 0) : list.get(0);
    }

    @Override
    public long crear(String numeroTicket, String titulo, String descripcion, String categoria, String prioridad, String creadoPor) {
        String sql = "INSERT INTO sga_soporte.tickets (numero_ticket, titulo, descripcion, categoria, prioridad, estado, creado_por, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, 'ABIERTO', ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id_ticket"});
            ps.setString(1, numeroTicket);
            ps.setString(2, titulo);
            ps.setString(3, descripcion);
            ps.setString(4, categoria);
            ps.setString(5, prioridad);
            ps.setString(6, creadoPor);
            return ps;
        }, keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    @Override
    public void actualizarEstado(long id, String estado, String asignadoA, String solucionAplicada) {
        if ("RESUELTO".equals(estado) || "CERRADO".equals(estado)) {
            String sql = "UPDATE sga_soporte.tickets SET estado = ?, asignado_a = COALESCE(?, asignado_a), solucion_aplicada = ?, fecha_resolucion = NOW() WHERE id_ticket = ?";
            jdbc.update(sql, estado, asignadoA, solucionAplicada, id);
        } else {
            String sql = "UPDATE sga_soporte.tickets SET estado = ?, asignado_a = COALESCE(?, asignado_a), solucion_aplicada = ? WHERE id_ticket = ?";
            jdbc.update(sql, estado, asignadoA, solucionAplicada, id);
        }
    }

    @Override
    public void actualizarPrioridadYAsignado(long id, String prioridad, String asignadoA) {
        if (prioridad != null && asignadoA != null) {
            jdbc.update("UPDATE sga_soporte.tickets SET prioridad = ?, asignado_a = ? WHERE id_ticket = ?", prioridad, asignadoA, id);
        } else if (prioridad != null) {
            jdbc.update("UPDATE sga_soporte.tickets SET prioridad = ? WHERE id_ticket = ?", prioridad, id);
        } else if (asignadoA != null) {
            jdbc.update("UPDATE sga_soporte.tickets SET asignado_a = ? WHERE id_ticket = ?", asignadoA, id);
        }
    }

    @Override
    public void registrarHistorial(long idTicket, String campo, String valorAnterior, String valorNuevo, String modificadoPor) {
        String sql = "INSERT INTO sga_soporte.historial_ticket (id_ticket, campo, valor_anterior, valor_nuevo, modificado_por, fecha_modificacion) VALUES (?, ?, ?, ?, ?, NOW())";
        jdbc.update(sql, idTicket, campo, valorAnterior, valorNuevo, modificadoPor);
    }

    @Override
    public List<Map<String, Object>> listarHistorial(long idTicket) {
        String sql = "SELECT id_historial AS \"id\", id_ticket AS \"idTicket\", campo, valor_anterior AS \"valorAnterior\", valor_nuevo AS \"valorNuevo\", modificado_por AS \"modificadoPor\", fecha_modificacion AS \"fechaModificacion\" FROM sga_soporte.historial_ticket WHERE id_ticket = ? ORDER BY id_historial DESC";
        return jdbc.queryForList(sql, idTicket);
    }

    @Override
    public List<Map<String, Object>> listarComentarios(long idTicket) {
        String sql = "SELECT id_comentario AS \"id\", id_ticket AS \"idTicket\", autor, contenido, nota_interna AS \"notaInterna\", fecha_creacion AS \"fechaCreacion\" FROM sga_soporte.comentarios WHERE id_ticket = ? ORDER BY id_comentario ASC";
        return jdbc.queryForList(sql, idTicket);
    }

    @Override
    public Map<String, Object> agregarComentario(long idTicket, String autor, String contenido, boolean notaInterna) {
        String sql = "INSERT INTO sga_soporte.comentarios (id_ticket, autor, contenido, nota_interna, fecha_creacion) VALUES (?, ?, ?, ?, NOW())";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id_comentario"});
            ps.setLong(1, idTicket);
            ps.setString(2, autor);
            ps.setString(3, contenido);
            ps.setBoolean(4, notaInterna);
            return ps;
        }, keyHolder);
        long idComentario = Objects.requireNonNull(keyHolder.getKey()).longValue();
        return Map.of("id", idComentario, "idTicket", idTicket, "autor", autor, "contenido", contenido, "notaInterna", notaInterna, "fechaCreacion", new Date());
    }
}
