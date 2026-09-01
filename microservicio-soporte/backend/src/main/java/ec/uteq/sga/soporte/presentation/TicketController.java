package ec.uteq.sga.soporte.controller;

import ec.uteq.sga.soporte.application.TicketService;
import ec.uteq.sga.soporte.dto.ActualizarTicketRequest;
import ec.uteq.sga.soporte.dto.ComentarioRequest;
import ec.uteq.sga.soporte.dto.EscalarTicketRequest;
import ec.uteq.sga.soporte.dto.TicketRequest;
import ec.uteq.sga.soporte.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * API del microservicio de soporte tecnico. Protegida por JwtAuthFilter
 * (autenticacion) sobre /api/soporte/*; las acciones exclusivas del equipo
 * de soporte se autorizan dentro de TicketService via AuthenticatedUser.
 */
@RestController
@RequestMapping("/api/soporte/tickets")
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> listar(AuthenticatedUser user) {
        return service.listar(user);
    }

    @GetMapping("/mis-tickets")
    public List<Map<String, Object>> misTickets(AuthenticatedUser user) {
        return service.misTickets(user.username());
    }

    @GetMapping("/estadisticas")
    public Map<String, Object> estadisticas(AuthenticatedUser user) {
        return service.estadisticas(user);
    }

    @GetMapping("/reportes")
    public Map<String, Object> reportes(AuthenticatedUser user) {
        return service.reportes(user);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable long id, AuthenticatedUser user) {
        return service.obtener(id, user);
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
        return service.actualizar(id, req, user);
    }

    @PostMapping("/{id}/escalar")
    public Map<String, Object> escalar(
            @PathVariable long id, @Valid @RequestBody EscalarTicketRequest req, AuthenticatedUser user) {
        return service.escalar(id, req, user);
    }

    @GetMapping("/{id}/historial")
    public List<Map<String, Object>> historial(@PathVariable long id, AuthenticatedUser user) {
        return service.listarHistorial(id, user);
    }

    @GetMapping("/{id}/comentarios")
    public List<Map<String, Object>> listarComentarios(@PathVariable long id) {
        return service.listarComentarios(id);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<Map<String, Object>> comentar(
            @PathVariable long id, @Valid @RequestBody ComentarioRequest req, AuthenticatedUser user) {
        Map<String, Object> creado = service.comentar(id, req, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}