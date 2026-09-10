package ec.edu.uteq.sga.presentation.controller;
import ec.edu.uteq.sga.application.service.*;
import ec.edu.uteq.sga.domain.dto.notificacion.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController @RequiredArgsConstructor
public class PushNotificationController {
 private final DispositivoRepresentanteService dispositivos; private final PushNotificationService push;
 @Value("${app.notifications.internal-token:}") private String internalToken;
 @PostMapping("/api/representante/me/dispositivos") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void registrar(Authentication auth,@Valid @RequestBody DispositivoRequestDTO dto){dispositivos.registrar(auth.getName(),dto);}
 @DeleteMapping("/api/representante/me/dispositivos") @ResponseStatus(HttpStatus.NO_CONTENT)
 public void eliminar(Authentication auth,@Valid @RequestBody DispositivoRequestDTO dto){dispositivos.eliminar(auth.getName(),dto);}
 @PostMapping("/api/internal/notificaciones/eventos") @ResponseStatus(HttpStatus.ACCEPTED)
 public void evento(@RequestHeader(value="X-Internal-Token",required=false) String token,@Valid @RequestBody EventoAcademicoPushDTO dto){
   if(internalToken.isBlank()||token==null||!MessageDigest.isEqual(internalToken.getBytes(StandardCharsets.UTF_8),token.getBytes(StandardCharsets.UTF_8))) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED);
   push.procesar(dto);
 }
}
