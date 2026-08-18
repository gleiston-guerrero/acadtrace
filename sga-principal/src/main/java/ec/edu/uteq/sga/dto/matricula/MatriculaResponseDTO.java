package ec.edu.uteq.sga.dto.matricula;

import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatriculaResponseDTO {
    private Long idMatricula;
    private Long idEstudiante;
    private String estudianteNombres;
    private String estudianteApellidos;
    private String estudianteCedula;
    private String estudianteCodigo;
    private Long idGrado;
    private String grado;
    private Long idParalelo;
    private String paralelo;
    private Long idAnoLectivo;
    private String anoLectivo;
    private Short numeroOrden;
    private LocalDate fechaRegistro;
    private String estado;
    private String observaciones;
    private String registradoPor;
    private Instant fechaCreacion;

    // Campos detallados de Estudiante y Representante
    private String direccionEstudiante;
    private String telefonoEstudiante;
    private String correoEstudiante;
    private String fechaNacimientoEstudiante;
    private String representanteNombre;
    private String representanteCedula;
    private String representanteParentesco;
    private String representanteTelefono;

    // Campos de Traslado
    private String institucionDestino;
    private String motivoTraslado;
}
