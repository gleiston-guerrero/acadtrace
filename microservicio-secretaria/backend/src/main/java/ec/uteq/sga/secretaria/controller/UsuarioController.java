package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.dto.AsignarRolesRequest;
import ec.uteq.sga.secretaria.dto.CambiarEstadoRequest;
import ec.uteq.sga.secretaria.dto.UsuarioRequest;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/usuarios")
public class UsuarioController {

    private final UsuarioService service;

    public UsuarioController(UsuarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return service.listarTodos();
    }

    @GetMapping("/roles")
    public List<Map<String, Object>> roles() {
        return service.listarRoles();
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody UsuarioRequest dto, AuthenticatedUser user) {
        requireDirector(user);
        return service.crear(dto);
    }

    @PatchMapping("/{id}/reset-password")
    public Map<String, Object> resetPassword(@PathVariable Long id, AuthenticatedUser user) {
        requireDirector(user);
        return service.resetearPassword(id);
    }

    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest dto, AuthenticatedUser user) {
        requireDirector(user);
        service.cambiarEstado(id, dto.estado());
    }

    @PatchMapping("/{id}/roles")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void asignarRoles(@PathVariable Long id, @Valid @RequestBody AsignarRolesRequest dto, AuthenticatedUser user) {
        requireDirector(user);
        service.asignarRoles(id, dto.roles());
    }

    /**
     * Gestion de usuarios (crear, resetear password, asignar roles, cambiar
     * estado) queda reservada a DIRECTOR; SECRETARIA solo puede listar/ver
     * (ver docs/STRIDE_SLA_secretaria.md). JwtAuthFilter ya deja pasar a
     * SECRETARIA y DIRECTOR a nivel de toda la ruta /api/secretario/*, asi
     * que sin este chequeo cualquier SECRETARIA podia escalar privilegios
     * via estos endpoints.
     */
    private void requireDirector(AuthenticatedUser user) {
        if (!user.isDirector()) {
            throw ApiException.forbidden("Requiere rol DIRECTOR");
        }
    }
}
