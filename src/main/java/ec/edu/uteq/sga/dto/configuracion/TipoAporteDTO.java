package ec.edu.uteq.sga.dto.configuracion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoAporteDTO {

    private Long idTipoAporte;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 60, message = "El nombre no puede superar 60 caracteres")
    private String nombre;

    @NotBlank(message = "El tipo de evaluación es obligatorio")
    private String tipoEvaluacion; // FORMATIVA o SUMATIVA

    private Integer orden;

    private Boolean activo;
}
