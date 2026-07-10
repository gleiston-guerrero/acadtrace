package ec.edu.uteq.sga.soporte.controller;

import ec.edu.uteq.sga.soporte.dto.ticket.*;
import ec.edu.uteq.sga.soporte.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/soporte/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<List<TicketResponseDTO>> listar() {
        return ResponseEntity.ok(ticketService.listarTodos());
    }

    @GetMapping("/mis-tickets")
    public ResponseEntity<List<TicketResponseDTO>> misTickets(Authentication auth) {
        return ResponseEntity.ok(ticketService.listarPorUsuario(auth.getName()));
    }

    @GetMapping("/asignados-a-mi")
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<List<TicketResponseDTO>> asignadosAMi(Authentication auth) {
        return ResponseEntity.ok(ticketService.listarAsignadosA(auth.getName()));
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<TicketResponseDTO>> buscar(@RequestParam String q) {
        return ResponseEntity.ok(ticketService.buscar(q));
    }

    @GetMapping("/estado/{estado}")
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<List<TicketResponseDTO>> porEstado(@PathVariable String estado) {
        return ResponseEntity.ok(ticketService.listarPorEstado(estado));
    }

    @GetMapping("/estadisticas")
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<Map<String, Long>> estadisticas() {
        return ResponseEntity.ok(ticketService.estadisticas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.obtenerPorId(id));
    }

    @PostMapping
    public ResponseEntity<TicketResponseDTO> crear(@Valid @RequestBody TicketRequestDTO dto,
                                                   Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.crear(dto, auth.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<TicketResponseDTO> actualizar(@PathVariable Long id,
                                                        @RequestBody TicketUpdateDTO dto) {
        return ResponseEntity.ok(ticketService.actualizar(id, dto));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyAuthority('SOPORTE_TECNICO','ADMINISTRADOR')")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id,
                                              @RequestParam String estado) {
        ticketService.cambiarEstado(id, estado);
        return ResponseEntity.noContent().build();
    }
}