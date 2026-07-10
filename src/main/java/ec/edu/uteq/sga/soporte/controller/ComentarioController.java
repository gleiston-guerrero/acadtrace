package ec.edu.uteq.sga.soporte.controller;

import ec.edu.uteq.sga.soporte.dto.comentario.*;
import ec.edu.uteq.sga.soporte.service.ComentarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/soporte/tickets/{idTicket}/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    @GetMapping
    public ResponseEntity<List<ComentarioResponseDTO>> listar(
            @PathVariable Long idTicket,
            @RequestParam(defaultValue = "false") boolean soloPublicos,
            Authentication auth) {
        return ResponseEntity.ok(comentarioService.listarPorTicket(idTicket, soloPublicos));
    }

    @PostMapping
    public ResponseEntity<ComentarioResponseDTO> agregar(
            @PathVariable Long idTicket,
            @Valid @RequestBody ComentarioRequestDTO dto,
            Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(comentarioService.agregar(idTicket, dto, auth.getName()));
    }
}