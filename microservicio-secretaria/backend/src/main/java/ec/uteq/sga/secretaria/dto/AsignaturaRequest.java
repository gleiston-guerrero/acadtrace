package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotBlank;

public record AsignaturaRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        String codigo,
        String descripcion,
        Integer horasSemanales
) {}
