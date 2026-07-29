package ec.edu.uteq.sga.dto.importacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExcelImportResultDTO {
    private List<ExcelEstudianteDTO> estudiantes;
    private int totalFilas;
    private int filasValidas;
    private int filasConError;
}
