package ec.edu.uteq.sga.domain.dto.importacion;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExcelEstudianteDTO {
    private Integer fila;
    private String cedula;
    private String apellidos;
    private String nombres;
    private String email;
    private boolean yaExiste;
    private String error;
}
