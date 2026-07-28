package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.dto.PromocionRequest;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.service.HistorialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/historial")
public class HistorialController {

    private final HistorialService service;

    public HistorialController(HistorialService service) {
        this.service = service;
    }

    @GetMapping("/estudiante/{id}")
    public Map<String, Object> historialEstudiante(@PathVariable Long id) {
        return service.historialEstudiante(id);
    }

    @GetMapping("/ano-lectivo/{idAno}/resumen")
    public List<Map<String, Object>> resumen(@PathVariable Long idAno) {
        return service.resumenPromocion(idAno);
    }

    @GetMapping("/ano-lectivo/{idAno}/sin-promocion")
    public List<Map<String, Object>> sinPromocion(@PathVariable Long idAno) {
        return service.estudiantesSinPromocion(idAno);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody PromocionRequest dto, AuthenticatedUser user) {
        return service.registrarPromocion(dto, user.username());
    }
}
