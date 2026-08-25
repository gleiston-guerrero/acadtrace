package ec.edu.uteq.sga.domain.entity.dto.importacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ConfirmarImportacionExcelDTO {
    private Long idGrado;
    private Long idParalelo;
    private Long idAnoLectivo;
    private List<ExcelEstudianteDTO> estudiantes;
}
