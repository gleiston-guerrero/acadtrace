package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RepresentanteRequest(
        String cedula,
        @NotBlank(message = "Nombres requeridos") String nombres,
        @NotBlank(message = "Apellidos requeridos") String apellidos,
        String parentesco,
        @NotBlank(message = "Teléfono principal requerido") String telefono_principal,
        String telefono_alt,
        @Email(message = "correo inválido") String correo,
        String direccion
) {}
