package ec.uteq.sga.secretaria.controller;

import ec.uteq.sga.secretaria.dto.ConfirmarImportacionRequest;
import ec.uteq.sga.secretaria.dto.ImportacionResultado;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.service.ImportacionEstudiantesService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/secretario/importacion-estudiantes")
public class ImportacionEstudiantesController {

    private final ImportacionEstudiantesService service;

    public ImportacionEstudiantesController(ImportacionEstudiantesService service) {
        this.service = service;
    }

    @PostMapping(value = "/parsear", consumes = "multipart/form-data")
    public ImportacionResultado parsear(@RequestParam("archivo") MultipartFile archivo) {
        return service.parsearArchivo(archivo);
    }

    @PostMapping("/confirmar")
    public Map<String, Object> confirmar(@RequestBody ConfirmarImportacionRequest request, AuthenticatedUser user) {
        return service.confirmarImportacion(request.estudiantes(), user.username(),
                request.id_grado(), request.id_paralelo(), request.id_ano_lectivo());
    }
}
