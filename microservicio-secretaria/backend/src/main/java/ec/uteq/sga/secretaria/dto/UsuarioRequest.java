package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UsuarioRequest(
        @NotBlank(message = "Username requerido") String username,
        @NotBlank(message = "Correo inválido") @Email(message = "Correo inválido") String correo,
        @NotBlank(message = "Nombres requeridos") String nombres,
        @NotBlank(message = "Apellidos requeridos") String apellidos,
        String cedula,
        String telefono,
        String cargo,
        String titulo_academico,
        String especializacion,
        List<String> roles
) {}
