package ec.uteq.sga.secretaria.presentation.controller;

import ec.uteq.sga.secretaria.domain.dto.EventoAcademicoRequest;
import ec.uteq.sga.secretaria.infrastructure.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.application.service.CalendarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/calendario")
public class CalendarioController {

    private final CalendarioService service;

    public CalendarioController(CalendarioService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> calendario(@RequestParam(name = "mes") String mes) {
        return service.calendario(mes);
    }

    @GetMapping("/eventos")
    public List<Map<String, Object>> listarEventos() {
        return service.listarEventos();
    }

    @PostMapping("/eventos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearEvento(@Valid @RequestBody EventoAcademicoRequest dto, AuthenticatedUser user) {
        return service.crearEvento(dto, user.username());
    }

    @PutMapping("/eventos/{id}")
    public Map<String, Object> actualizarEvento(@PathVariable Long id, @Valid @RequestBody EventoAcademicoRequest dto) {
        return service.actualizarEvento(id, dto);
    }

    @DeleteMapping("/eventos/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarEvento(@PathVariable Long id) {
        service.eliminarEvento(id);
    }
}
