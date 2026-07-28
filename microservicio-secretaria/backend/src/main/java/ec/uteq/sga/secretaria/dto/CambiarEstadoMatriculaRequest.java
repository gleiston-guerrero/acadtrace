package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CambiarEstadoMatriculaRequest(
        @NotBlank(message = "estado requerido")
        @Pattern(regexp = "ACTIVA|RETIRADA|EGRESADA|PROMOVIDA|NO_PROMOVIDA", message = "estado inválido")
        String estado
) {}
