package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/test/email")
@RequiredArgsConstructor
public class EmailTestController {

    private final EmailService emailService;

    @PostMapping("/enviar")
    public ResponseEntity<?> probarEnvioDe(@RequestParam String correo) {
        try {
            emailService.enviarCredenciales(
                    correo,
                    "Test Usuario",
                    "test.user",
                    "TestPass123!"
            );
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Correo enviado a: " + correo
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage(),
                    "tipo", e.getClass().getSimpleName()
            ));
        }
    }
}
