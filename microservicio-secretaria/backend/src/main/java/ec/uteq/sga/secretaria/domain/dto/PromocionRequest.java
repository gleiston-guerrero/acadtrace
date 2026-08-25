package ec.uteq.sga.secretaria.domain.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PromocionRequest(
        @JsonProperty("id_matricula")
        @JsonAlias({"idMatricula", "id_matricula"})
        @NotNull(message = "id_matricula requerido")
        Long id_matricula,

        @JsonProperty("resultado")
        @NotNull(message = "resultado inválido")
        @Pattern(regexp = "PROMOVIDO|REPROBADO|NO_PROMOVIDO|RETIRADO|TRASLADADO", message = "resultado inválido")
        String resultado,

        @JsonProperty("promedio_anual")
        @JsonAlias({"promedioAnual", "promedio_anual"})
        @DecimalMin(value = "0", message = "promedio_anual inválido")
        @DecimalMax(value = "10", message = "promedio_anual inválido")
        Double promedio_anual,

        @JsonProperty("observaciones")
        String observaciones
) {}
