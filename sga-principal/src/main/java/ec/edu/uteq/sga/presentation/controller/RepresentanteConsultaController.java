package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.application.service.RepresentanteConsultaService;
import ec.edu.uteq.sga.domain.dto.representante.RepresentadoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/representante/me")
@RequiredArgsConstructor
public class RepresentanteConsultaController {
    private final RepresentanteConsultaService service;

    @GetMapping("/estudiantes")
    public List<RepresentadoResponseDTO> listar(Authentication authentication) {
        return service.listar(authentication.getName());
    }

    @GetMapping("/estudiantes/{idEstudiante}/autorizacion")
    public RepresentadoResponseDTO autorizar(Authentication authentication, @PathVariable Long idEstudiante) {
        return service.autorizar(authentication.getName(), idEstudiante);
    }

    @GetMapping("/estudiantes/{idEstudiante}/calificaciones")
    public java.util.Map<String, Object> calificaciones(Authentication authentication, @PathVariable Long idEstudiante) {
        return service.calificaciones(authentication.getName(), idEstudiante);
    }

    @GetMapping("/estudiantes/{idEstudiante}/asistencia")
    public java.util.Map<String, Object> asistencia(Authentication authentication, @PathVariable Long idEstudiante) {
        return service.asistencia(authentication.getName(), idEstudiante);
    }

    @GetMapping("/comunicados")
    public java.util.List<java.util.Map<String, Object>> comunicados(Authentication authentication) {
        return service.comunicados(authentication.getName());
    }
}
