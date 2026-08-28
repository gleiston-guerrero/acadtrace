package ec.edu.uteq.sga.domain.entity.dto.asignacion;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AsignacionResponseDTO {
    private Long idAsignacion;
    private String docente;
    private String asignatura;
    private String grado;
    private String paralelo;
    private String anoLectivo;
    private boolean esTutor;
    private boolean activo;
    private Instant fechaAsignacion;
    private String asignadoPor;

    private Long idDocente;
    private Long idAsignatura;
    private Long idGrado;
    private Long idParalelo;
    private Long idAnoLectivo;
    private String cedulaDocente;
    private String correoDocente;
    private String tituloDocente;
    private String fotoDocente;
    private Integer horasSemanales;
}