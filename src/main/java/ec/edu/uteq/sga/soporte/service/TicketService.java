package ec.edu.uteq.sga.soporte.service;

import ec.edu.uteq.sga.soporte.dto.ticket.*;
import ec.edu.uteq.sga.soporte.entity.TicketSoporte;
import ec.edu.uteq.sga.soporte.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepo;

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarTodos() {
        return ticketRepo.findAllByOrderByFechaCreacionDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarPorUsuario(String username) {
        return ticketRepo.findByCreadoPorOrderByFechaCreacionDesc(username)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarAsignadosA(String username) {
        return ticketRepo.findByAsignadoAOrderByFechaCreacionDesc(username)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> listarPorEstado(String estado) {
        return ticketRepo.findByEstadoOrderByFechaCreacionDesc(estado)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> buscar(String q) {
        return ticketRepo
                .findByTituloContainingIgnoreCaseOrDescripcionContainingIgnoreCase(q, q)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO obtenerPorId(Long id) {
        return toDTO(buscarPorId(id));
    }

    @Transactional
    public TicketResponseDTO crear(TicketRequestDTO dto, String username) {
        String numero = generarNumero();

        TicketSoporte ticket = TicketSoporte.builder()
                .numeroTicket(numero)
                .titulo(dto.getTitulo())
                .descripcion(dto.getDescripcion())
                .prioridad(dto.getPrioridad() != null ? dto.getPrioridad() : "MEDIO")
                .categoria(dto.getCategoria() != null ? dto.getCategoria() : "OTRO")
                .estado("ABIERTO")
                .creadoPor(username)
                .build();

        return toDTO(ticketRepo.save(ticket));
    }

    @Transactional
    public TicketResponseDTO actualizar(Long id, TicketUpdateDTO dto) {
        TicketSoporte ticket = buscarPorId(id);

        if (dto.getEstado() != null) {
            ticket.setEstado(dto.getEstado());
            if ("RESUELTO".equals(dto.getEstado()) || "CERRADO".equals(dto.getEstado())) {
                ticket.setFechaResolucion(Instant.now());
            }
        }
        if (dto.getAsignadoA() != null) {
            ticket.setAsignadoA(dto.getAsignadoA());
            if ("ABIERTO".equals(ticket.getEstado())) {
                ticket.setEstado("EN_PROCESO");
            }
        }
        if (dto.getSolucionAplicada() != null) {
            ticket.setSolucionAplicada(dto.getSolucionAplicada());
        }

        ticket.setFechaActualizacion(Instant.now());
        return toDTO(ticketRepo.save(ticket));
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        TicketSoporte ticket = buscarPorId(id);
        ticket.setEstado(estado);
        ticket.setFechaActualizacion(Instant.now());
        if ("RESUELTO".equals(estado) || "CERRADO".equals(estado)) {
            ticket.setFechaResolucion(Instant.now());
        }
        ticketRepo.save(ticket);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> estadisticas() {
        return Map.of(
                "abiertos",  ticketRepo.contarPorEstado("ABIERTO"),
                "enProceso", ticketRepo.contarPorEstado("EN_PROCESO"),
                "resueltos", ticketRepo.contarPorEstado("RESUELTO"),
                "cerrados",  ticketRepo.contarPorEstado("CERRADO"),
                "total",     ticketRepo.count()
        );
    }

    private TicketSoporte buscarPorId(Long id) {
        return ticketRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));
    }

    private String generarNumero() {
        Long maxId = ticketRepo.maxId();
        long siguiente = (maxId == null ? 0L : maxId) + 1;
        return String.format("TKT-%04d", siguiente);
    }

    private TicketResponseDTO toDTO(TicketSoporte t) {
        return TicketResponseDTO.builder()
                .idTicket(t.getIdTicket())
                .numeroTicket(t.getNumeroTicket())
                .titulo(t.getTitulo())
                .descripcion(t.getDescripcion())
                .prioridad(t.getPrioridad())
                .estado(t.getEstado())
                .categoria(t.getCategoria())
                .creadoPor(t.getCreadoPor())
                .asignadoA(t.getAsignadoA())
                .solucionAplicada(t.getSolucionAplicada())
                .fechaCreacion(t.getFechaCreacion())
                .fechaActualizacion(t.getFechaActualizacion())
                .fechaResolucion(t.getFechaResolucion())
                .build();
    }
}