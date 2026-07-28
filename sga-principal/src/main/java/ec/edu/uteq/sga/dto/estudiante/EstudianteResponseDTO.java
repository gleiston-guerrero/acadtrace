package ec.edu.uteq.sga.dto.estudiante;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstudianteResponseDTO {
    private Long idEstudiante;
    private String cedula;
    private String codigoEstudiante;
    private String nombres;
    private String apellidos;
    private LocalDate fechaNacimiento;
    private String genero;
    private String direccion;
    private String telefono;
    private String correo;
    private boolean discapacidad;
    private String tipoDiscapacidad;
    private Short porcentajeDisc;
    private String carnetConadis;
    private String nacionalidad;
    private String etnia;
    private String lugarNacimiento;
    private String viveCon;
    private Short numerosHermanos;
    private boolean beneficioSocial;
    private String estado;
    private String fotoUrl;
    private String origenListado;
    private String representante;
    private Long idRepresentante;
    private Instant fechaCreacion;

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