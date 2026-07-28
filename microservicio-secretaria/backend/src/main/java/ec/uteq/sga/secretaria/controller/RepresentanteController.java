package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.dto.RepresentanteRequest;
import ec.uteq.sga.secretaria.service.RepresentanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/representantes")
public class RepresentanteController {

    private final RepresentanteService service;

    public RepresentanteController(RepresentanteService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(name = "q", required = false) String search) {
        return service.listarTodos(search);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody RepresentanteRequest dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody RepresentanteRequest dto) {
        return service.actualizar(id, dto);
    }
}
