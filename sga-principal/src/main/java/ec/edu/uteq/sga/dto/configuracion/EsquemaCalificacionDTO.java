package ec.edu.uteq.sga.dto.configuracion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EsquemaCalificacionDTO {

    private Long idEsquema;

    @NotNull(message = "El peso de la evaluación formativa es obligatorio")
    @DecimalMin(value = "0.00", message = "El peso no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El peso no puede superar 100")
    private BigDecimal pesoFormativa;

    @NotNull(message = "El peso de la evaluación sumativa es obligatorio")
    @DecimalMin(value = "0.00", message = "El peso no puede ser negativo")
    @DecimalMax(value = "100.00", message = "El peso no puede superar 100")
    private BigDecimal pesoSumativa;
}
