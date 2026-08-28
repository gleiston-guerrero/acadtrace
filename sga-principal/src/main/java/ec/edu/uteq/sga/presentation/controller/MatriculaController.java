package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.dto.matricula.MatriculaRequestDTO;
import ec.edu.uteq.sga.domain.dto.matricula.MatriculaResponseDTO;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import ec.edu.uteq.sga.application.service.MatriculaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matriculas")
@RequiredArgsConstructor
public class MatriculaController {

    private final MatriculaService matriculaService;
    private final UsuarioRepository usuarioRepo;

    @GetMapping
    public ResponseEntity<MatriculaService.PaginaMatriculas> listar(
            @RequestParam(required = false) Long idAnoLectivo,
            @RequestParam(required = false) Long idEstudiante,
            @RequestParam(required = false) Long idGrado,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "500") int limit) {
        return ResponseEntity.ok(matriculaService.listar(idAnoLectivo, idEstudiante, idGrado, q, page, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MatriculaResponseDTO> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(matriculaService.obtener(id));
    }

    @PostMapping
    public ResponseEntity<MatriculaResponseDTO> crear(@Valid @RequestBody MatriculaRequestDTO dto, Authentication auth) {
        Long idUsuario = usuarioRepo.findByUsername(auth.getName()).map(u -> u.getIdUsuario()).orElse(null);
        return ResponseEntity.status(HttpStatus.CREATED).body(matriculaService.crear(dto, idUsuario));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<Void> cambiarEstado(@PathVariable Long id, @RequestParam String estado, @RequestParam(required = false) String observaciones) {
        matriculaService.cambiarEstado(id, estado, observaciones);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> descargarPdf(@PathVariable Long id) {
        byte[] pdf = matriculaService.generarPdfMatricula(id);
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Ficha_Matricula_" + id + ".pdf\"")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
