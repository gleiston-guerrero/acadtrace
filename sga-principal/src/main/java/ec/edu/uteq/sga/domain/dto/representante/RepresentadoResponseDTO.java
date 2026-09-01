package ec.edu.uteq.sga.domain.dto.representante;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RepresentadoResponseDTO {
    private Long idEstudiante;
    private String nombres;
    private String apellidos;
    private String curso;
    private String paralelo;
    private List<Long> matriculas;
}
