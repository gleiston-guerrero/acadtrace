package ec.edu.uteq.sga.domain.entity.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RepresentanteInputDTO {
    private String cedula;
    private String nombres;
    private String apellidos;
    private String parentesco;
    private String telefonoPrincipal;
    private String telefonoAlt;
    private String correo;
    private String direccion;
}
