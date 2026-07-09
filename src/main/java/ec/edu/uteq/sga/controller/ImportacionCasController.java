package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.dto.importacion.CasPdfResultDTO;
import ec.edu.uteq.sga.dto.importacion.ConfirmarImportacionDTO;
import ec.edu.uteq.sga.service.ImportacionCasService;
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
