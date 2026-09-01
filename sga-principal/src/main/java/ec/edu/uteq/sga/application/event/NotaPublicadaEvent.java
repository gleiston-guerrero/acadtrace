package ec.edu.uteq.sga.application.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Evento de dominio emitido cuando se asienta una calificación en el SGA.
 */
public class NotaPublicadaEvent {

    private final Long idEstudiante;
    private final String cedulaEstudiante;
    private final String materia;
    private final BigDecimal calificacion;
    private final String docente;
    private final LocalDateTime fechaEmision;

    public NotaPublicadaEvent(Long idEstudiante, String cedulaEstudiante, String materia, BigDecimal calificacion, String docente) {
        this.idEstudiante = idEstudiante;
        this.cedulaEstudiante = cedulaEstudiante;
        this.materia = materia;
        this.calificacion = calificacion;
        this.docente = docente;
        this.fechaEmision = LocalDateTime.now();
    }

    public Long getIdEstudiante() { return idEstudiante; }
    public String getCedulaEstudiante() { return cedulaEstudiante; }
    public String getMateria() { return materia; }
    public BigDecimal getCalificacion() { return calificacion; }
    public String getDocente() { return docente; }
    public LocalDateTime getFechaEmision() { return fechaEmision; }
}