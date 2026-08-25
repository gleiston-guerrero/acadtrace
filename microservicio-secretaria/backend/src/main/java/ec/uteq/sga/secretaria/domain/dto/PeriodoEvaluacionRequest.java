package ec.uteq.sga.secretaria.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record PeriodoEvaluacionRequest(
        @NotBlank(message = "El nombre del periodo es obligatorio") String nombre,
        String tipo,
        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate fechaInicio,
        @NotNull(message = "La fecha de fin es obligatoria") LocalDate fechaFin,
        Integer orden
) {}