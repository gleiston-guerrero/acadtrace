package ec.uteq.sga.soporte.controller;

import ec.uteq.sga.soporte.dto.ActualizarTicketRequest;
import ec.uteq.sga.soporte.dto.ComentarioRequest;
import ec.uteq.sga.soporte.dto.TicketRequest;
import ec.uteq.sga.soporte.security.AuthenticatedUser;
import ec.uteq.sga.soporte.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API del microservicio de soporte tecnico. Protegida por JwtAuthFilter
 * (roles SOPORTE_TECNICO / DIRECTOR) sobre /api/soporte/*.
 */
@RestController
@RequestMapping("/api/soporte/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return service.listar();
    }

    @GetMapping("/mis-tickets")
    public List<Map<String, Object>> misTickets(AuthenticatedUser user) {
        return service.misTickets(user.username());
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas() {
        return service.estadisticas();
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> crear(
            @Valid @RequestBody TicketRequest req, AuthenticatedUser user) {
        Map<String, Object> creado = service.crear(req, user.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

@PutMapping("/{id}")
public Map<String, Object> actualizar(
        @PathVariable long id, @Valid @RequestBody ActualizarTicketRequest req, AuthenticatedUser user) {
    return service.actualizar(id, req, user.username());
}

@GetMapping("/{id}/historial")
public List<Map<String, Object>> historial(@PathVariable long id) {
    return service.listarHistorial(id);
}

    @GetMapping("/{id}/comentarios")
    public List<Map<String, Object>> listarComentarios(@PathVariable long id) {
        return service.listarComentarios(id);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<Map<String, Object>> comentar(
            @PathVariable long id, @Valid @RequestBody ComentarioRequest req, AuthenticatedUser user) {
        Map<String, Object> creado = service.comentar(id, req, user.username());
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
