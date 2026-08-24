package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/secretario/uploads")
public class FotoUploadController {

    private static final Logger log = LoggerFactory.getLogger(FotoUploadController.class);
    private static final long MAX_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${app.uploads.dir:uploads}")
    private String baseDir;

    @PostMapping(value = "/foto", consumes = "multipart/form-data")
    public Map<String, String> subirFoto(@RequestParam("archivo") MultipartFile archivo) {
        if (archivo.isEmpty()) {
            throw ApiException.badRequest("El archivo proporcionado está vacío");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw ApiException.badRequest("El tamaño máximo permitido es de 5 MB");
        }
        String ct = archivo.getContentType() == null ? "" : archivo.getContentType().toLowerCase();
        if (!(ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp"))) {
            throw ApiException.badRequest("Formato no soportado. Solo se permiten JPG, PNG o WEBP");
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
            log.error("Error al guardar la imagen en el servidor: ", e);
            throw ApiException.internal("No se pudo guardar la imagen: " + e.getMessage());
        }

        return Map.of("url", "/uploads/fotos/" + nombre);
    }

    @GetMapping("/fotos/{filename:.+}")
    public ResponseEntity<Resource> verFoto(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(baseDir, "fotos", filename).toAbsolutePath().normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) {
                    contentType = "application/octet-stream";
                }
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                throw ApiException.notFound("Foto no encontrada");
            }
        } catch (IOException e) {
            throw ApiException.notFound("Foto no encontrada");
        }
    }
}