package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record MatriculaRequest(
        @NotNull(message = "id_estudiante requerido") Long id_estudiante,
        @NotNull(message = "id_grado requerido") Long id_grado,
        @NotNull(message = "id_paralelo requerido") Long id_paralelo,
        @NotNull(message = "id_ano_lectivo requerido") Long id_ano_lectivo,
        @Pattern(regexp = "ACTIVA|RETIRADA|EGRESADA|PROMOVIDA|NO_PROMOVIDA", message = "estado inválido") String estado,
        String observaciones
) {}
