package ec.uteq.sga.soporte.domain.port;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Puerto (patron Repository) hacia la persistencia de tickets. El negocio
 * (application/TicketService) depende de esta interfaz, no de JdbcTemplate
 * directamente. La implementacion real vive en infrastructure/persistence/
 * (JdbcTicketRepository), usando el mismo SQL que ya existia.
 *
 * Las lecturas devuelven Map<String,Object> con alias camelCase a proposito:
 * es la misma forma que hoy consume el frontend, y convertir cada proyeccion
 * a un objeto Ticket completo perderia esa flexibilidad sin aportar valor
 * real al negocio. Las escrituras si trabajan con parametros explicitos.
 */
public interface TicketRepositoryPort {

    List<Map<String, Object>> listarTodos();

    List<Map<String, Object>> listarPorCreador(String username);

    Optional<Map<String, Object>> buscarPorId(long id);

    Map<String, Object> estadisticas();

    List<Map<String, Object>> reportePorCategoria();

    List<Map<String, Object>> reportePorTecnico();

    Map<String, Object> reporteGeneral();

    /** Inserta un ticket nuevo (estado ABIERTO) y devuelve el id generado. */
    long crear(String numeroTicket, String titulo, String descripcion,
               String categoria, String prioridad, String creadoPor);

    void actualizarEstado(long id, String estado, String asignadoA, String solucionAplicada);

    void actualizarPrioridadYAsignado(long id, String prioridad, String asignadoA);

    void registrarHistorial(long idTicket, String campo, String valorAnterior, String valorNuevo, String modificadoPor);

    List<Map<String, Object>> listarHistorial(long idTicket);

    List<Map<String, Object>> listarComentarios(long idTicket);

    /** Inserta un comentario y devuelve el comentario ya guardado (con su id). */
    Map<String, Object> agregarComentario(long idTicket, String autor, String contenido, boolean notaInterna);
}
