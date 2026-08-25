package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.dto.notificacion.NotificacionMasivoRequestDTO;
import ec.edu.uteq.sga.domain.dto.notificacion.NotificacionMiasResponseDTO;
import ec.edu.uteq.sga.application.service.NotificacionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @GetMapping("/mias")
    public ResponseEntity<NotificacionMiasResponseDTO> mias(Authentication auth) {
        return ResponseEntity.ok(notificacionService.mias(auth));
    }

    @PostMapping("/marcar-leida/{id}")
    public ResponseEntity<Void> marcarLeida(Authentication auth, @PathVariable Long id) {
        notificacionService.marcarLeida(auth, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/marcar-todas-leidas")
    public ResponseEntity<Void> marcarTodasLeidas(Authentication auth) {
        notificacionService.marcarTodasLeidas(auth);
        return ResponseEntity.noContent().build();
    }

    /**
     * Interno: llamado por otros microservicios (secretaria/docente/soporte) para crear
     * notificaciones masivas. Sin JWT de usuario final (permitAll en SecurityConfig) —
     * pensado solo para llamadas intra-red, igual que /api/rpc/** ya funciona hoy.
     */
    @PostMapping("/masivo")
    @ResponseStatus(HttpStatus.CREATED)
    public void masivo(@Valid @RequestBody NotificacionMasivoRequestDTO dto) {
        notificacionService.enviarMasivo(dto);
    }
}
