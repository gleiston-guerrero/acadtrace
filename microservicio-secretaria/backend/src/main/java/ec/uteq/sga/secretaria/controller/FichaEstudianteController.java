package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.dto.FichaEstudianteRequest;
import ec.uteq.sga.secretaria.service.FichaEstudianteService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/secretario/estudiantes/{idEstudiante}/ficha")
public class FichaEstudianteController {

    private final FichaEstudianteService service;

    public FichaEstudianteController(FichaEstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public Map<String, Object> obtener(@PathVariable Long idEstudiante) {
        return service.obtenerPorEstudiante(idEstudiante);
    }

    @PutMapping
    public Map<String, Object> guardar(@PathVariable Long idEstudiante, @Valid @RequestBody FichaEstudianteRequest dto) {
        return service.guardar(idEstudiante, dto);
    }
}
