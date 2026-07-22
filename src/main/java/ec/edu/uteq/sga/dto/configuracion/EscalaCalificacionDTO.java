package ec.edu.uteq.sga.dto.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EscalaCalificacionDTO {

    private Long idEscala;

    @NotNull(message = "El nivel educativo es obligatorio")
    private Long idNivel;

    private String nivel;

    @NotNull(message = "La nota mínima es obligatoria")
    private BigDecimal notaMinima;

    @NotNull(message = "La nota máxima es obligatoria")
    private BigDecimal notaMaxima;

    @NotBlank(message = "El equivalente cualitativo es obligatorio")
    @Size(max = 5, message = "El equivalente no puede superar 5 caracteres")
    private String equivalenteCualitativo;

    @Size(max = 100, message = "La descripción no puede superar 100 caracteres")
    private String descripcion;
}
