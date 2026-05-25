package ec.edu.uteq.sga.dto.grado;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GradoResponseDTO {
    private Long idGrado;
    private String nombre;
    private Short nivel;
    private String paralelo;
    private Short capacidadMax;
    private boolean activo;
    private String nivelEducativo;
}