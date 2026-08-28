package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.domain.dto.AsignaturaRequest;
import ec.uteq.sga.secretaria.application.service.AsignaturaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/asignaturas")
public class AsignaturaController {

    private final AsignaturaService asignaturaService;

    public AsignaturaController(AsignaturaService asignaturaService) {
        this.asignaturaService = asignaturaService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return asignaturaService.listarTodos();
    }

    @GetMapping("/activas")
    public List<Map<String, Object>> listarActivas() {
        return asignaturaService.listarActivas();
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable long id) {
        return asignaturaService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody AsignaturaRequest dto) {
        return asignaturaService.crear(dto);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable long id, @Valid @RequestBody AsignaturaRequest dto) {
        return asignaturaService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarEstado(@PathVariable long id, @RequestParam boolean activo) {
        asignaturaService.cambiarEstado(id, activo);
    }
}
