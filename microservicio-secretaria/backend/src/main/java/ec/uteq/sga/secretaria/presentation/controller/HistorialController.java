package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.domain.dto.PromocionRequest;
import ec.uteq.sga.secretaria.infrastructure.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.application.service.HistorialService;
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

    @GetMapping("/ano-lectivo/{idAno}/nomina")
    public List<Map<String, Object>> nomina(
            @PathVariable Long idAno,
            @RequestParam(required = false) Long id_grado,
            @RequestParam(required = false) Long id_paralelo,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String q) {
        return service.listarPromociones(idAno, id_grado, id_paralelo, estado, q);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> registrar(@Valid @RequestBody PromocionRequest dto, AuthenticatedUser user) {
        return service.registrarPromocion(dto, user.username());
    }

    @PostMapping("/masivo")
    public Map<String, Object> registrarMasivo(@RequestBody List<@Valid PromocionRequest> dtos, AuthenticatedUser user) {
        return service.registrarPromocionMasiva(dtos, user.username());
    }

    @DeleteMapping("/{idHistorial}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long idHistorial) {
        service.eliminarPromocion(idHistorial);
    }
}
