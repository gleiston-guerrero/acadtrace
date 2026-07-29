package ec.edu.uteq.sga.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CrearEstudianteDTO {
    private String cedula;
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
    private RepresentanteInputDTO representante;

    /** Vincula un representante ya existente (por id) — usado por el flujo gRPC de sga-secretaria,
     *  que crea/gestiona representantes por su cuenta. Tiene prioridad sobre {@link #representante}. */
    private Long idRepresentante;

    /** Usuario que originó el registro (resuelto por sga-secretaria antes de llamar por gRPC). */
    private Long idUsuarioCreador;

    /** Código generado por sga-secretaria (EST-0001...); en blanco = no tocar el código actual (caso actualizar). */
    private String codigoEstudiante;
}
