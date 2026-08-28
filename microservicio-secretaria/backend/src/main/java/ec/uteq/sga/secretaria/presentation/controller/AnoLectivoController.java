package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.domain.dto.AnoLectivoRequest;
import ec.uteq.sga.secretaria.domain.dto.PeriodoEvaluacionRequest;
import ec.uteq.sga.secretaria.application.service.AnoLectivoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/anos-lectivos")
public class AnoLectivoController {

    private final AnoLectivoService anoLectivoService;

    public AnoLectivoController(AnoLectivoService anoLectivoService) {
        this.anoLectivoService = anoLectivoService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return anoLectivoService.listarTodos();
    }

    @GetMapping("/actual")
    public Map<String, Object> obtenerActual() {
        return anoLectivoService.obtenerActual();
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable long id) {
        return anoLectivoService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody AnoLectivoRequest dto) {
        return anoLectivoService.crear(dto);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable long id, @Valid @RequestBody AnoLectivoRequest dto) {
        return anoLectivoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activar(@PathVariable long id) {
        anoLectivoService.establecerActual(id);
    }

    @GetMapping("/{id}/periodos")
    public List<Map<String, Object>> listarPeriodos(@PathVariable long id) {
        return anoLectivoService.listarPeriodos(id);
    }

    @PostMapping("/{id}/periodos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearPeriodo(@PathVariable long id, @Valid @RequestBody PeriodoEvaluacionRequest dto) {
        return anoLectivoService.crearPeriodo(id, dto);
    }

    @PutMapping("/periodos/{idPeriodo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarPeriodo(@PathVariable long idPeriodo, @Valid @RequestBody PeriodoEvaluacionRequest dto) {
        anoLectivoService.actualizarPeriodo(idPeriodo, dto);
    }

    @DeleteMapping("/periodos/{idPeriodo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPeriodo(@PathVariable long idPeriodo) {
        anoLectivoService.eliminarPeriodo(idPeriodo);
    }
}