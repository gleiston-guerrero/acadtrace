package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.service.CalificacionConsultaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/calificaciones")
public class CalificacionConsultaController {

    private final CalificacionConsultaService calificacionService;

    public CalificacionConsultaController(CalificacionConsultaService calificacionService) {
        this.calificacionService = calificacionService;
    }

    @GetMapping("/cursos")
    public List<Map<String, Object>> listarCursos(
            @RequestParam(required = false) Long idAnoLectivo,
            @RequestParam(required = false) Long idGrado) {
        return calificacionService.listarCursos(idAnoLectivo, idGrado);
    }

    @GetMapping("/periodos")
    public List<Map<String, Object>> listarPeriodos(@RequestParam(required = false) Long idAnoLectivo) {
        return calificacionService.listarPeriodos(idAnoLectivo);
    }

    @GetMapping("/matriz/{idAsignacion}")
    public Map<String, Object> obtenerMatriz(
            @PathVariable long idAsignacion,
            @RequestParam(required = false) Long idPeriodo) {
        return calificacionService.obtenerMatrizNotas(idAsignacion, idPeriodo);
    }
}