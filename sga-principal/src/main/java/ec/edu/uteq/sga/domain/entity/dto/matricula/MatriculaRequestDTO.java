package ec.edu.uteq.sga.domain.entity.dto.matricula;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatriculaRequestDTO {

    @NotNull(message = "El estudiante es obligatorio")
    private Long idEstudiante;

    @NotNull(message = "El grado es obligatorio")
    private Long idGrado;

    @NotNull(message = "El paralelo es obligatorio")
    private Long idParalelo;

    @NotNull(message = "El año lectivo es obligatorio")
    private Long idAnoLectivo;

    private String estado;

    private String observaciones;
}