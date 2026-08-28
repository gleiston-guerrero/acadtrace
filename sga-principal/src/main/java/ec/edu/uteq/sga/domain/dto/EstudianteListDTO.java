package ec.edu.uteq.sga.domain.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstudianteListDTO {
    private Long idEstudiante;
    private String cedula;
    private String codigoEstudiante;
    private String nombres;
    private String apellidos;
    private String correo;
    private String estado;
    private String origenListado;
    private String representante;
    private String fotoUrl;
}
