package ec.uteq.sga.secretaria.domain.dto;

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
        String direccion,
        String fecha_nacimiento,
        String genero,
        String estado_civil,
        String nacionalidad,
        String ocupacion,
        String lugar_trabajo,
        String telefono_trabajo,
        String cargo,
        String nivel_instruccion,
        java.math.BigDecimal ingreso_mensual,
        Boolean convive_con_estudiante,
        String contacto_emergencia_nombre,
        String contacto_emergencia_telefono,
        String observaciones
) {}
