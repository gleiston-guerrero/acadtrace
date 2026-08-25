package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.dto.AuthResponse;
import ec.edu.uteq.sga.domain.dto.LoginRequest;
import ec.edu.uteq.sga.domain.dto.usuario.CambioPasswordDTO;
import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import ec.edu.uteq.sga.infrastructure.security.JwtUtil;
import ec.edu.uteq.sga.application.service.AuditoriaService;
import ec.edu.uteq.sga.application.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService; // ← agregar esto
    private final AuditoriaService auditoriaService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (AuthenticationException e) {
            auditoriaService.registrarAuth("LOGIN_FALLIDO", request.getUsername(), null, "FALLO",
                    "Credenciales invalidas: " + e.getMessage());
            throw e;
        }

        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow();

        List<String> roles = usuario.getRoles().stream()
                .map(r -> r.getNombre())
                .collect(Collectors.toList());

        String token = jwtUtil.generarToken(usuario.getUsername(), roles);

        auditoriaService.registrarAuth("LOGIN", usuario.getUsername(), usuario.getIdUsuario(), "EXITO", null);

        return ResponseEntity.ok(AuthResponse.builder()
                .token(token)
                .idUsuario(usuario.getIdUsuario())
                .username(usuario.getUsername())
                .correo(usuario.getCorreo())
                .roles(roles)
                .primerIngreso(usuario.isPrimerIngreso())
                .build());
    }

    @PatchMapping("/cambiar-password")
    public ResponseEntity<Void> cambiarPassword(
            Authentication auth,
            @Valid @RequestBody CambioPasswordDTO dto) {
        usuarioService.cambiarPassword(auth.getName(), dto);
        auditoriaService.registrarAuth("CAMBIO_PASSWORD", auth.getName(), null, "EXITO", null);
        return ResponseEntity.noContent().build();
    }
}
