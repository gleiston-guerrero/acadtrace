package ec.edu.uteq.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class FotoUploadController {

    private static final long MAX_BYTES = 3 * 1024 * 1024;

    @Value("${app.uploads.dir:uploads}")
    private String baseDir;

    @PostMapping(value = "/foto", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, String>> subir(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Máximo 3 MB");
        }
        String ct = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Solo JPG, PNG o WEBP");
        }

        String ext = switch (ct) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
        String nombre = UUID.randomUUID() + ext;

        try {
            Path dir = Paths.get(baseDir, "fotos").toAbsolutePath();
            Files.createDirectories(dir);
            Path destino = dir.resolve(nombre);
            archivo.transferTo(destino.toFile());
        } catch (IOException e) {
            log.error("Error al guardar la imagen en servidor: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen: " + e.getMessage());
        }

        return ResponseEntity.ok(Map.of("url", "/uploads/fotos/" + nombre));
    }
}
