package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.dto.configuracion.*;
import ec.edu.uteq.sga.service.ConfiguracionCalificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Configuración del esquema de calificación del año lectivo activo:
 * trimestres, ponderación formativa/sumativa, tipos de aporte y escala cualitativa.
 *
 * Sólo se guarda la configuración; las notas viven en el microservicio docente.
 */
@RestController
@RequestMapping("/api/configuracion/calificacion")
@RequiredArgsConstructor
public class ConfiguracionCalificacionController {

    private final ConfiguracionCalificacionService service;

    // ---------------------------------------------------------------- esquema

    @GetMapping("/esquema")
    public ResponseEntity<EsquemaCalificacionDTO> obtenerEsquema() {
        return ResponseEntity.ok(service.obtenerEsquema());
    }

    @PutMapping("/esquema")
    public ResponseEntity<EsquemaCalificacionDTO> guardarEsquema(
            @Valid @RequestBody EsquemaCalificacionDTO dto) {
        return ResponseEntity.ok(service.guardarEsquema(dto));
    }

    // ----------------------------------------------------------- tipos aporte

    @GetMapping("/aportes")
    public ResponseEntity<List<TipoAporteDTO>> listarAportes() {
        return ResponseEntity.ok(service.listarTiposAporte());
    }

    @PostMapping("/aportes")
    public ResponseEntity<TipoAporteDTO> crearAporte(@Valid @RequestBody TipoAporteDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearTipoAporte(dto));
    }

    @PutMapping("/aportes/{id}")
    public ResponseEntity<TipoAporteDTO> actualizarAporte(
            @PathVariable Long id, @Valid @RequestBody TipoAporteDTO dto) {
        return ResponseEntity.ok(service.actualizarTipoAporte(id, dto));
    }

    @DeleteMapping("/aportes/{id}")
    public ResponseEntity<Void> eliminarAporte(@PathVariable Long id) {
        service.eliminarTipoAporte(id);
        return ResponseEntity.noContent().build();
    }

    // ----------------------------------------------------------------- escala

    @GetMapping("/escala")
    public ResponseEntity<List<EscalaCalificacionDTO>> listarEscala(
            @RequestParam(required = false) Long idNivel) {
        return ResponseEntity.ok(service.listarEscala(idNivel));
    }

    @PostMapping("/escala")
    public ResponseEntity<EscalaCalificacionDTO> crearRango(@Valid @RequestBody EscalaCalificacionDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearRangoEscala(dto));
    }

    @PutMapping("/escala/{id}")
    public ResponseEntity<EscalaCalificacionDTO> actualizarRango(
            @PathVariable Long id, @Valid @RequestBody EscalaCalificacionDTO dto) {
        return ResponseEntity.ok(service.actualizarRangoEscala(id, dto));
    }

    @DeleteMapping("/escala/{id}")
    public ResponseEntity<Void> eliminarRango(@PathVariable Long id) {
        service.eliminarRangoEscala(id);
        return ResponseEntity.noContent().build();
    }

    /** Convierte una nota cuantitativa a su equivalente cualitativo. */
    @GetMapping("/escala/convertir")
    public ResponseEntity<EscalaCalificacionDTO> convertir(
            @RequestParam Long idNivel, @RequestParam BigDecimal nota) {
        return ResponseEntity.ok(service.convertirACualitativa(idNivel, nota));
    }

    // --------------------------------------------------------------- periodos

    @GetMapping("/periodos")
    public ResponseEntity<List<PeriodoEvaluacionDTO>> listarPeriodos() {
        return ResponseEntity.ok(service.listarPeriodos());
    }

    @PutMapping("/periodos/{id}")
    public ResponseEntity<PeriodoEvaluacionDTO> actualizarPeriodo(
            @PathVariable Long id, @Valid @RequestBody PeriodoEvaluacionDTO dto) {
        return ResponseEntity.ok(service.actualizarPeriodo(id, dto));
    }
}
