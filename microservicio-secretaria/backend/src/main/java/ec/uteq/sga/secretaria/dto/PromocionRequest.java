package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PromocionRequest(
        @NotNull(message = "id_matricula requerido") Long id_matricula,
        @NotNull(message = "resultado inválido")
        @Pattern(regexp = "PROMOVIDO|NO_PROMOVIDO|RETIRADO", message = "resultado inválido")
        String resultado,
        @DecimalMin(value = "0", message = "promedio_anual inválido")
        @DecimalMax(value = "10", message = "promedio_anual inválido")
        Double promedio_anual,
        String observaciones
) {}
