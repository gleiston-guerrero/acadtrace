package ec.edu.uteq.sga.dto.grado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParaleloDTO {
    private Long idParalelo;
    private String letra;
    private boolean activo;
    private Long idGrado;
    private String nombreGrado;
    private long totalEstudiantes;
}
