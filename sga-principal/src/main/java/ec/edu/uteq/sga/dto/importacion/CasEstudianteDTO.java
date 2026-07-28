package ec.edu.uteq.sga.dto.importacion;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CasEstudianteDTO {
    private Integer fila;
    private String cedula;
    private String apellidos;
    private String nombres;
    private String email;
    private boolean yaExiste;
}
