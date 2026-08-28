package ec.uteq.sga.soporte.application;

import ec.uteq.sga.soporte.common.ApiException;
import ec.uteq.sga.soporte.domain.port.TicketRepositoryPort;
import ec.uteq.sga.soporte.dto.ActualizarTicketRequest;
import ec.uteq.sga.soporte.dto.ComentarioRequest;
import ec.uteq.sga.soporte.dto.EscalarTicketRequest;
import ec.uteq.sga.soporte.dto.TicketRequest;
import ec.uteq.sga.soporte.security.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Logica de negocio del modulo de soporte tecnico. Ya no habla con
 * JdbcTemplate directamente: todo el acceso a datos pasa por
 * TicketRepositoryPort (implementado en infrastructure/persistence/
 * JdbcTicketRepository). Esta clase solo valida reglas de negocio y
 * orquesta las llamadas al puerto.
 */
@Service
public class TicketService {

    private static final Set<String> ESTADOS = Set.of("ABIERTO", "EN_PROCESO", "RESUELTO", "CERRADO");
    private static final Set<String> PRIORIDADES = Set.of("BAJO", "MEDIO", "ALTO", "CRITICO");
    private static final Set<String> CATEGORIAS = Set.of("HARDWARE", "SOFTWARE", "RED", "CUENTA", "OTRO");

    private final TicketRepositoryPort tickets;

    public TicketService(TicketRepositoryPort tickets) {
        this.tickets = tickets;
    }

    public List<Map<String, Object>> listar(AuthenticatedUser user) {
        exigirTecnicoODirector(user);
        return tickets.listarTodos();
    }

    public List<Map<String, Object>> misTickets(String username) {
        return tickets.listarPorCreador(username);
    }

    public Map<String, Object> estadisticas(AuthenticatedUser user) {
        exigirTecnicoODirector(user);
        return tickets.estadisticas();
    }

    /** Reportes de gestion: totales y tiempo promedio de resolucion (horas) por categoria y tecnico. */
    public Map<String, Object> reportes(AuthenticatedUser user) {
        exigirTecnicoODirector(user);
        return Map.of(
                "porCategoria", tickets.reportePorCategoria(),
                "porTecnico", tickets.reportePorTecnico(),
                "general", tickets.reporteGeneral()
        );
    }

    /** Lanza 403 si el usuario no es del equipo de soporte (SOPORTE_TECNICO/DIRECTOR/ADMINISTRADOR). */
    private void exigirTecnicoODirector(AuthenticatedUser user) {
        if (user == null || !user.isTecnicoOrDirector()) {
            throw ApiException.forbidden("Accion exclusiva del equipo de soporte");
        }
    }

    public Map<String, Object> obtener(long id) {
        return tickets.buscarPorId(id)
                .orElseThrow(() -> ApiException.notFound("Ticket no encontrado"));
    }

    public Map<String, Object> obtener(long id, AuthenticatedUser user) {
        Map<String, Object> ticket = obtener(id);
        if (user == null || !user.isTecnicoOrDirector()) {
            String creadoPor = (String) ticket.get("creadoPor");
            if (user == null || creadoPor == null || !creadoPor.equals(user.username())) {
                throw ApiException.forbidden("No tiene acceso a este ticket");
            }
        }
        return ticket;
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
        long id = tickets.crear(numero, req.titulo(), req.descripcion(), categoria, prioridad, creadoPor);
        return obtener(id);
    }

    /**
     * Creacion automatica de tickets reportados por OTRO microservicio
     * (Principal, Docente, Secretaria) via gRPC (ver IncidenciaGrpcServer),
     * cuando ese servicio detecta una falla propia. A diferencia de crear(),
     * es tolerante: si la categoria/prioridad que manda el servicio de
     * origen no es una de las validas, cae a un valor por defecto en lugar
     * de rechazar el reporte -- perder una incidencia real por un typo de
     * otro equipo es peor que clasificarla mal.
     */
    @Transactional
    public Map<String, Object> crearDesdeIncidencia(String servicioOrigen, String titulo, String descripcion,
                                                      String categoriaSugerida, String prioridadSugerida) {
        String categoria = categoriaSugerida == null ? "" : categoriaSugerida.toUpperCase();
        if (!CATEGORIAS.contains(categoria)) categoria = "OTRO";

        String prioridad = prioridadSugerida == null ? "" : prioridadSugerida.toUpperCase();
        if (!PRIORIDADES.contains(prioridad)) prioridad = "ALTO"; // falla reportada por un servicio: se asume urgente

        String creadoPor = "sistema:" + (servicioOrigen == null || servicioOrigen.isBlank() ? "desconocido" : servicioOrigen);
        String numero = "TK-" + System.currentTimeMillis();
        long id = tickets.crear(numero, titulo, descripcion, categoria, prioridad, creadoPor);
        return obtener(id);
    }

    @Transactional
    public Map<String, Object> actualizar(long id, ActualizarTicketRequest req, AuthenticatedUser user) {
        exigirTecnicoODirector(user);
        String modificadoPor = user.username();
        String estado = req.estado().toUpperCase();
        if (!ESTADOS.contains(estado)) {
            throw ApiException.badRequest("Estado inválido (ABIERTO, EN_PROCESO, RESUELTO, CERRADO)");
        }

        Map<String, Object> actual = obtener(id); // valida existencia y trae valores previos
        String estadoAnterior = (String) actual.get("estado");
        String asignadoAnterior = (String) actual.get("asignadoA");

        tickets.actualizarEstado(id, estado, req.asignadoA(), req.solucionAplicada());

        if (!estado.equals(estadoAnterior)) {
            tickets.registrarHistorial(id, "ESTADO", estadoAnterior, estado, modificadoPor);
        }
        if (req.asignadoA() != null && !req.asignadoA().equals(asignadoAnterior)) {
            tickets.registrarHistorial(id, "ASIGNADO_A", asignadoAnterior, req.asignadoA(), modificadoPor);
        }

        return obtener(id);
    }

    /**
     * Escalamiento: sube la prioridad y/o reasigna el ticket a un tecnico
     * superior. Requiere al menos uno de los dos cambios y siempre deja el
     * motivo registrado como nota interna en el timeline del ticket.
     */
    @Transactional
    public Map<String, Object> escalar(long id, EscalarTicketRequest req, AuthenticatedUser user) {
        exigirTecnicoODirector(user);
        String actor = user.username();
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

        tickets.actualizarPrioridadYAsignado(id, nuevaPrioridad, nuevoAsignado);

        if (nuevaPrioridad != null && !nuevaPrioridad.equals(prioridadAnterior)) {
            tickets.registrarHistorial(id, "PRIORIDAD", prioridadAnterior, nuevaPrioridad, actor);
        }
        if (nuevoAsignado != null && !nuevoAsignado.equals(asignadoAnterior)) {
            tickets.registrarHistorial(id, "ASIGNADO_A", asignadoAnterior, nuevoAsignado, actor);
        }

        tickets.agregarComentario(id, actor, "Escalamiento: " + req.motivo(), true);

        return obtener(id);
    }

    public List<Map<String, Object>> listarHistorial(long idTicket, AuthenticatedUser user) {
        obtener(idTicket, user); // valida existencia y autorizacion
        return tickets.listarHistorial(idTicket);
    }

    public List<Map<String, Object>> listarComentarios(long idTicket) {
        obtener(idTicket); // valida existencia
        return tickets.listarComentarios(idTicket);
    }

    @Transactional
    public Map<String, Object> comentar(long idTicket, ComentarioRequest req, AuthenticatedUser user) {
        obtener(idTicket); // valida existencia
        boolean notaInterna = req.notaInterna() != null && req.notaInterna() && user.isTecnicoOrDirector();
        return tickets.agregarComentario(idTicket, user.username(), req.contenido(), notaInterna);
    }
}