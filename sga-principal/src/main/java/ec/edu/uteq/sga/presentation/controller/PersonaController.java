package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.dto.persona.PersonaRequestDTO;
import ec.edu.uteq.sga.domain.dto.persona.PersonaResponseDTO;
import ec.edu.uteq.sga.application.service.PersonaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personas")
@RequiredArgsConstructor
public class PersonaController {

    private final PersonaService personaService;

    @GetMapping
    public ResponseEntity<List<PersonaResponseDTO>> listar() {
        return ResponseEntity.ok(personaService.listar());
    }

    @GetMapping("/buscar")
    public ResponseEntity<PersonaResponseDTO> buscarPorCedula(@RequestParam String cedula) {
        return ResponseEntity.ok(personaService.buscarPorCedula(cedula));
    }

    @GetMapping("/usuario/{idUsuario}")
    public ResponseEntity<PersonaResponseDTO> porUsuario(@PathVariable Long idUsuario) {
        return ResponseEntity.ok(personaService.obtenerPorUsuario(idUsuario));
    }

    @PostMapping
    public ResponseEntity<PersonaResponseDTO> crear(@Valid @RequestBody PersonaRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(personaService.crear(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PersonaResponseDTO> actualizar(@PathVariable Long id,
                                                         @Valid @RequestBody PersonaRequestDTO dto) {
        return ResponseEntity.ok(personaService.actualizar(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        personaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
