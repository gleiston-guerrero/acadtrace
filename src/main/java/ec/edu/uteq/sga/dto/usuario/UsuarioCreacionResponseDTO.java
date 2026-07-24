package ec.edu.uteq.sga.dto.usuario;

import lombok.*;
import java.time.Instant;
import java.util.Set;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UsuarioCreacionResponseDTO {
    private Long idUsuario;
    private String username;
    private String correo;
    private String passwordTemporal;
    private boolean estado;
    private boolean primerIngreso;
    private Set<String> roles;
    private Instant fechaCreacion;
    private String mensaje;
}
