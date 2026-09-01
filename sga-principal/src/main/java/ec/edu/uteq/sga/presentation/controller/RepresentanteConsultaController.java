package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.application.service.RepresentanteConsultaService;
import ec.edu.uteq.sga.domain.dto.representante.RepresentadoResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/representante/me/estudiantes")
@RequiredArgsConstructor
public class RepresentanteConsultaController {
    private final RepresentanteConsultaService service;

    @GetMapping
    public List<RepresentadoResponseDTO> listar(Authentication authentication) {
        return service.listar(authentication.getName());
    }

    @GetMapping("/{idEstudiante}/autorizacion")
    public RepresentadoResponseDTO autorizar(Authentication authentication, @PathVariable Long idEstudiante) {
        return service.autorizar(authentication.getName(), idEstudiante);
    }
}
