package ec.edu.uteq.sga.domain.dto.importacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfirmarImportacionDTO {
    private Long idGrado;
    private Long idParalelo;
    private Long idAnoLectivo;
    private List<CasEstudianteDTO> estudiantes;
}
