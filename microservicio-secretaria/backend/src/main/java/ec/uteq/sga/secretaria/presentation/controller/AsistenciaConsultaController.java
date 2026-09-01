package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.application.service.AsistenciaConsultaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Lectura de asistencias para Secretaria. /api/docente/asistencias en sga-principal
 * esta restringido a ROLE_DOCENTE; este endpoint expone la misma informacion (lectura,
 * sin escritura) por SQL directo, igual que ya hace ReportesService para el PDF mensual. */
@RestController
@RequestMapping("/api/secretario/asistencias")
public class AsistenciaConsultaController {

    private final AsistenciaConsultaService service;

    public AsistenciaConsultaController(AsistenciaConsultaService service) {
        this.service = service;
    }

    @GetMapping("/asignacion/{idAsignacion}")
    public Map<String, Object> porAsignacion(@PathVariable long idAsignacion) {
        return Map.of("asistencias", service.porAsignacion(idAsignacion));
    }
}
