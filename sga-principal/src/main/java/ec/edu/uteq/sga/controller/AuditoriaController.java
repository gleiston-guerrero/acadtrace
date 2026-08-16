package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.dto.auditoria.AuditoriaResponseDTO;
import ec.edu.uteq.sga.service.AuditoriaService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auditoria")
@RequiredArgsConstructor
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    @GetMapping
    public ResponseEntity<Page<AuditoriaResponseDTO>> buscar(
            @RequestParam(required = false) String schemaOrigen,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false) String tablaAfectada,
            @RequestParam(required = false) String resultado,
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
        return ResponseEntity.ok(auditoriaService.buscar(schemaOrigen, accion, tablaAfectada, resultado, username, pageable));
    }

    @GetMapping("/trace/{traceId}")
    public ResponseEntity<List<AuditoriaResponseDTO>> porTrace(@PathVariable UUID traceId) {
        return ResponseEntity.ok(auditoriaService.porTrace(traceId));
    }
}
