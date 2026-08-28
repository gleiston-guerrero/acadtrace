package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.dto.importacion.CasPdfResultDTO;
import ec.edu.uteq.sga.domain.dto.importacion.ConfirmarImportacionDTO;
import ec.edu.uteq.sga.application.service.ImportacionCasService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/importacion-cas")
@RequiredArgsConstructor
public class ImportacionCasController {

    private final ImportacionCasService importacionService;

    @PostMapping("/parsear")
    public ResponseEntity<CasPdfResultDTO> parsearPdf(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(importacionService.parsearPdf(archivo));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmar(@RequestBody ConfirmarImportacionDTO dto) {
        return ResponseEntity.ok(importacionService.confirmarImportacion(dto));
    }
}
