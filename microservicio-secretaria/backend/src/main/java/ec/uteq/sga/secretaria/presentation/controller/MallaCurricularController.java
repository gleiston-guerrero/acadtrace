package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.domain.dto.ActualizarHorasGradoRequest;
import ec.uteq.sga.secretaria.domain.dto.MallaRequest;
import ec.uteq.sga.secretaria.application.service.MallaCurricularService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/secretario/malla")
public class MallaCurricularController {

    private final MallaCurricularService mallaService;

    public MallaCurricularController(MallaCurricularService mallaService) {
        this.mallaService = mallaService;
    }

    @GetMapping("/grado/{idGrado}")
    public Map<String, Object> porGrado(@PathVariable long idGrado, @RequestParam long idAnoLectivo) {
        return mallaService.porGrado(idGrado, idAnoLectivo);
    }

    @GetMapping("/resumen-grados")
    public Map<Long, Map<String, Object>> resumenGrados(@RequestParam long idAnoLectivo) {
        return mallaService.resumenGrados(idAnoLectivo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void agregar(@Valid @RequestBody MallaRequest req) {
        mallaService.agregar(req);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void actualizarHoras(@PathVariable long id, @RequestParam short horasSemana) {
        mallaService.actualizarHoras(id, horasSemana);
    }

    @PostMapping("/actualizar-horas-grado")
    public void actualizarHorasGrado(@Valid @RequestBody ActualizarHorasGradoRequest req) {
        mallaService.actualizarHorasGrado(req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable long id) {
        mallaService.eliminar(id);
    }
}
