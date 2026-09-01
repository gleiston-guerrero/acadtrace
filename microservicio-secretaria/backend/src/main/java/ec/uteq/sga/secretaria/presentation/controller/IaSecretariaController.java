package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.application.service.IaSecretariaService;
import ec.uteq.sga.secretaria.domain.dto.IaChatRequest;
import ec.uteq.sga.secretaria.domain.dto.IaCitacionRequest;
import ec.uteq.sga.secretaria.domain.dto.IaDiagnosticoRequest;
import ec.uteq.sga.secretaria.domain.dto.IaDiagnosticoResponse;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/secretario/ia")
public class IaSecretariaController {

    private final IaSecretariaService iaService;

    public IaSecretariaController(IaSecretariaService iaService) {
        this.iaService = iaService;
    }

    @PostMapping("/diagnostico-estudiante")
    public IaDiagnosticoResponse diagnosticar(@RequestBody IaDiagnosticoRequest req) {
        return iaService.generarDiagnostico(req);
    }

    @GetMapping("/diagnostico-matricula/{idMatricula}")
    public IaDiagnosticoResponse diagnosticarPorMatricula(@PathVariable Long idMatricula) {
        return iaService.diagnosticoPorMatricula(idMatricula);
    }

    @PostMapping("/asistente")
    public Map<String, Object> asistenteChat(@RequestBody IaChatRequest req) {
        return iaService.procesarConsultaChat(req);
    }

    @PostMapping("/generar-citacion")
    public Map<String, Object> generarCitacion(@RequestBody IaCitacionRequest req) {
        return iaService.generarBorradorCitacion(req);
    }
}