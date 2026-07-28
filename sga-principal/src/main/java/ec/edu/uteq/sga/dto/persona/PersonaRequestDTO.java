package ec.edu.uteq.sga.dto.persona;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PersonaRequestDTO {

    private Long idUsuario;

    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "\\d{10}", message = "La cédula debe tener 10 dígitos")
    private String cedula;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    private LocalDate fechaNacimiento;

    @Pattern(regexp = "MASCULINO|FEMENINO|OTRO|", message = "Género inválido")
    private String genero;

    @Size(max = 20)
    private String telefono;

    @Size(max = 20)
    private String telefonoAlt;

    private String direccion;

    @Size(max = 100)
    private String correoPersonal;

    @Size(max = 100)
    private String tituloAcademico;

    @Size(max = 100)
    private String especializacion;

    private String fotoUrl;
}
