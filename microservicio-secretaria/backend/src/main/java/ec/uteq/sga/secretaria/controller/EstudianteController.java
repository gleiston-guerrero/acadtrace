package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.PageResult;
import ec.uteq.sga.secretaria.dto.CambiarEstadoRequest;
import ec.uteq.sga.secretaria.dto.EstudianteRequest;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.service.EstudianteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/secretario/estudiantes")
public class EstudianteController {

    private final EstudianteService service;

    public EstudianteController(EstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public PageResult<Map<String, Object>> listar(
            @RequestParam(name = "q", required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "15") int limit) {
        return service.listarTodos(search, page, limit);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody EstudianteRequest dto, AuthenticatedUser user) {
        return service.crear(dto, user.username());
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody EstudianteRequest dto) {
        return service.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest dto, AuthenticatedUser user) {
        requireDirector(user);
        service.cambiarEstado(id, dto.estado());
    }

    /** Dar de baja/reactivar un estudiante queda reservado a DIRECTOR; SECRETARIA solo crea y edita. */
    private void requireDirector(AuthenticatedUser user) {
        if (!user.isDirector()) {
            throw ApiException.forbidden("Requiere rol DIRECTOR");
        }
    }
}
