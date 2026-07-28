package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.PageResult;
import ec.uteq.sga.secretaria.dto.CambiarEstadoMatriculaRequest;
import ec.uteq.sga.secretaria.dto.MatriculaRequest;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.service.MatriculaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/matriculas")
public class MatriculaController {

    private final MatriculaService service;

    public MatriculaController(MatriculaService service) {
        this.service = service;
    }

    @GetMapping("/paralelos/{idGrado}")
    public List<Map<String, Object>> paralelos(@PathVariable Long idGrado) {
        return service.paralelosPorGrado(idGrado);
    }

    @GetMapping("/ano-lectivo/{idAno}")
    public PageResult<Map<String, Object>> listarPorAno(
            @PathVariable Long idAno,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int limit,
            @RequestParam(name = "q", required = false) String search) {
        return service.listarPorAnoLectivo(idAno, page, limit, search);
    }

    @GetMapping("/ano-lectivo/{idAno}/estadisticas")
    public List<Map<String, Object>> estadisticas(@PathVariable Long idAno) {
        return service.estadisticasPorGrado(idAno);
    }

    @GetMapping("/estudiante/{idEstudiante}")
    public List<Map<String, Object>> porEstudiante(@PathVariable Long idEstudiante) {
        return service.listarPorEstudiante(idEstudiante);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody MatriculaRequest dto, AuthenticatedUser user) {
        return service.crear(dto, user.username());
    }

    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoMatriculaRequest dto, AuthenticatedUser user) {
        requireDirector(user);
        service.cambiarEstado(id, dto.estado());
    }

    /** Cambiar el estado de una matricula (retirar, promover...) queda reservado a DIRECTOR; SECRETARIA solo crea. */
    private void requireDirector(AuthenticatedUser user) {
        if (!user.isDirector()) {
            throw ApiException.forbidden("Requiere rol DIRECTOR");
        }
    }
}
