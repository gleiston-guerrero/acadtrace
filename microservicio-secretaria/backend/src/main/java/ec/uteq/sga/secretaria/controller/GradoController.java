package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.dto.GradoRequest;
import ec.uteq.sga.secretaria.service.GradoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/grados")
public class GradoController {

    private final GradoService gradoService;

    public GradoController(GradoService gradoService) {
        this.gradoService = gradoService;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return gradoService.listarTodos();
    }

    @GetMapping("/{idGrado}/paralelos")
    public List<Map<String, Object>> listarParalelos(@PathVariable Long idGrado) {
        return gradoService.listarParalelos(idGrado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody GradoRequest dto) {
        return gradoService.crear(dto);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable long id, @Valid @RequestBody GradoRequest dto) {
        return gradoService.actualizar(id, dto);
    }

    @PatchMapping("/{id}/estado")
    public void cambiarEstado(@PathVariable long id, @RequestParam boolean activo) {
        gradoService.cambiarEstado(id, activo);
    }

    @PostMapping("/{idGrado}/paralelos")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crearParalelo(@PathVariable long idGrado, @RequestParam String letra) {
        return gradoService.crearParalelo(idGrado, letra);
    }

    @PatchMapping("/paralelos/{idParalelo}/estado")
    public void cambiarEstadoParalelo(@PathVariable long idParalelo, @RequestParam boolean activo) {
        gradoService.cambiarEstadoParalelo(idParalelo, activo);
    }
}
