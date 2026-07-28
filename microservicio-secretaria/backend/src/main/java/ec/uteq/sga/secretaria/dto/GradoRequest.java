package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GradoRequest(
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotNull(message = "El orden es obligatorio") Integer orden,
        Integer capacidad_max,
        Long id_nivel
) {}
