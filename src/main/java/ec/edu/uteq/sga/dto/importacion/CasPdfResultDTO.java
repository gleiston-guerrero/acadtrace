package ec.edu.uteq.sga.dto.importacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CasPdfResultDTO {
    private String anoEscolar;
    private String paralelo;
    private String anoLectivo;
    private String regimen;
    private String jornada;
    private String institucion;
    private List<CasEstudianteDTO> estudiantes;
}
