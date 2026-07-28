package ec.edu.uteq.sga.dto.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PeriodoEvaluacionDTO {

    private Long idPeriodo;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo; // PRIMER_TRIMESTRE, SEGUNDO_TRIMESTRE, TERCER_TRIMESTRE

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    @NotNull(message = "La fecha de inicio es obligatoria")
    private LocalDate fechaInicio;

    @NotNull(message = "La fecha de fin es obligatoria")
    private LocalDate fechaFin;

    private Boolean activo;
}
