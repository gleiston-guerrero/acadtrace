package ec.edu.uteq.sga.dto.estudiante;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstudianteRequestDTO {

    @Size(max = 20)
    private String cedula;

    @NotBlank(message = "Los nombres son obligatorios")
    @Size(max = 100)
    private String nombres;

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    private String apellidos;

    private LocalDate fechaNacimiento;

    @Size(max = 1)
    private String genero;

    private String direccion;

    @Size(max = 20)
    private String telefono;

    @Email
    @Size(max = 100)
    private String correo;

    private boolean discapacidad = false;

    private String tipoDiscapacidad;

    private Short porcentajeDisc;

    private String carnetConadis;

    private String nacionalidad;

    private String etnia;

    private String lugarNacimiento;

    private String viveCon;

    private Short numerosHermanos;

    private boolean beneficioSocial = false;

    private Long idRepresentante;

    // Ficha médica
    private String tipoSangre;
    private String alergias;
    private String enfermedadesCronicas;
    private String medicamentos;
    private String contactoEmergenciaNombre;
    private String contactoEmergenciaTelefono;
    private String contactoEmergenciaParentesco;
    private String observacionesMedicas;
}