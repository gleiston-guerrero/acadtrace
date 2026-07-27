package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.dto.EstudianteListDTO;
import ec.edu.uteq.sga.service.EstudianteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * La ficha completa de estudiante (representantes, matrículas, documentos)
 * es dominio de sga-secretaria (ver principal.proto). Este controller opera
 * sobre el espejo legacy en sga_principal.estudiantes — el mismo que
 * alimentan los imports de respaldo (CAS/Excel/CSV/PDF, ver
 * ImportacionExcelService) y el registro manual rápido de este endpoint.
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteController {

    private final EstudianteService estudianteService;

    @GetMapping
    public ResponseEntity<List<EstudianteListDTO>> listar(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(estudianteService.listar(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstudianteDetalleDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(estudianteService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<EstudianteListDTO> crear(@RequestBody CrearEstudianteDTO dto) {
        return ResponseEntity.ok(estudianteService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<EstudianteListDTO> actualizar(@PathVariable Long id, @RequestBody CrearEstudianteDTO dto) {
        return ResponseEntity.ok(estudianteService.actualizar(id, dto));
    }
}
