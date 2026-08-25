package ec.edu.uteq.sga.presentation.controller;

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

    private static final long MAX_BYTES = 10 * 1024 * 1024;

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

    @GetMapping("/banners")
    public ResponseEntity<Map<String, String>> obtenerBanners() {
        Path dir = Paths.get(baseDir, "banners").toAbsolutePath();
        String b1 = null;
        String b2 = null;
        try {
            if (Files.exists(dir.resolve("banner1.txt"))) {
                b1 = Files.readString(dir.resolve("banner1.txt"));
            }
            if (Files.exists(dir.resolve("banner2.txt"))) {
                b2 = Files.readString(dir.resolve("banner2.txt"));
            }
        } catch (IOException e) {
            log.error("Error al leer banners en servidor: ", e);
        }
        Map<String, String> res = new java.util.HashMap<>();
        if (b1 != null) res.put("banner1", b1);
        if (b2 != null) res.put("banner2", b2);
        return ResponseEntity.ok(res);
    }

    @PostMapping(value = "/banner/{slot}")
    public ResponseEntity<Map<String, String>> guardarBanner(
            @PathVariable("slot") int slot,
            @RequestBody Map<String, String> body) {
        String data = body.get("data");
        if (data == null || data.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Datos vacíos");
        }
        try {
            Path dir = Paths.get(baseDir, "banners").toAbsolutePath();
            Files.createDirectories(dir);
            Path destino = dir.resolve("banner" + slot + ".txt");
            Files.writeString(destino, data);
            log.info("Banner institucional {} sincronizado y almacenado correctamente.", slot);
        } catch (IOException e) {
            log.error("Error al guardar banner en servidor: ", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar el banner");
        }
        return ResponseEntity.ok(Map.of("status", "OK", "slot", String.valueOf(slot)));
    }

    @DeleteMapping("/banner/{slot}")
    public ResponseEntity<Map<String, String>> eliminarBanner(@PathVariable("slot") int slot) {
        try {
            Path dir = Paths.get(baseDir, "banners").toAbsolutePath();
            Path destino = dir.resolve("banner" + slot + ".txt");
            Files.deleteIfExists(destino);
            log.info("Banner institucional {} eliminado.", slot);
        } catch (IOException e) {
            log.error("Error al eliminar banner en servidor: ", e);
        }
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
