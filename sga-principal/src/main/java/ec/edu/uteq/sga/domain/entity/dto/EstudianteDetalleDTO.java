package ec.edu.uteq.sga.domain.entity.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EstudianteDetalleDTO {
    private Long idEstudiante;
    private String cedula;
    private String codigoEstudiante;
    private String nombres;
    private String apellidos;
    private String correo;
    private String telefono;
    private String telefonoAlt;
    private LocalDate fechaNacimiento;
    private String genero;
    private String direccion;
    private String nacionalidad;
    private String etnia;
    private String lugarNacimiento;
    private String viveCon;
    private Short numerosHermanos;
    private boolean beneficioSocial;
    private boolean discapacidad;
    private String tipoDiscapacidad;
    private Short porcentajeDisc;
    private String carnetConadis;
    private String fotoUrl;
    private String estado;
    private String origenListado;
    private RepresentanteInputDTO representante;
    private Long idRepresentante;
}
