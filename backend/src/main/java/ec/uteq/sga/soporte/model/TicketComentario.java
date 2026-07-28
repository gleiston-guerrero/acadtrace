package ec.uteq.sga.soporte.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("ticket_comentarios")
public class TicketComentario {

    @Id
    private Long id;

    @Column("ticket_id")
    private Long ticketId;

    @Column("usuario_id")
    private Long usuarioId;

    private String comentario;

    @Column("es_respuesta_interna")
    private Boolean esRespuestaInterna;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    public TicketComentario() {
        this.fechaCreacion = LocalDateTime.now();
        this.esRespuestaInterna = false;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }

    public Boolean getEsRespuestaInterna() { return esRespuestaInterna; }
    public void setEsRespuestaInterna(Boolean esRespuestaInterna) { this.esRespuestaInterna = esRespuestaInterna; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}