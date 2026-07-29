package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.dto.importacion.ConfirmarImportacionExcelDTO;
import ec.edu.uteq.sga.dto.importacion.ExcelImportResultDTO;
import ec.edu.uteq.sga.service.ImportacionExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/importacion-excel")
@RequiredArgsConstructor
public class ImportacionExcelController {

    private final ImportacionExcelService importacionService;

    @PostMapping("/parsear")
    public ResponseEntity<ExcelImportResultDTO> parsearArchivo(@RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(importacionService.parsearArchivo(archivo));
    }

    @PostMapping("/confirmar")
    public ResponseEntity<Map<String, Object>> confirmar(@RequestBody ConfirmarImportacionExcelDTO dto) {
        return ResponseEntity.ok(importacionService.confirmarImportacion(dto));
    }
}
