package ec.edu.uteq.sga.domain.dto.persona;

import lombok.*;
import java.time.LocalDate;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonaResponseDTO {
    private Long idPersona;
    private Long idUsuario;
    private String username;
    private String correo;
    private Set<String> roles;
    private String cedula;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String genero;
    private String telefono;
    private String telefonoAlt;
    private String direccion;
    private String correoPersonal;
    private String tituloAcademico;
    private String especializacion;
    private String fotoUrl;
}
