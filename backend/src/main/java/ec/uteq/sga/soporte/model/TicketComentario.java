package ec.uteq.sga.soporte.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/** Entidad Spring Data JDBC que mapea sga_soporte.comentarios (no conectada a ningun controller hoy). */
@Table("sga_soporte.comentarios")
public class TicketComentario {

    @Id
    @Column("id_comentario")
    private Long id;

    @Column("id_ticket")
    private Long idTicket;

    private String autor;
    private String contenido;

    @Column("nota_interna")
    private Boolean notaInterna;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    public TicketComentario() {
        this.fechaCreacion = LocalDateTime.now();
        this.notaInterna = false;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdTicket() { return idTicket; }
    public void setIdTicket(Long idTicket) { this.idTicket = idTicket; }

    public String getAutor() { return autor; }
    public void setAutor(String autor) { this.autor = autor; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public Boolean getNotaInterna() { return notaInterna; }
    public void setNotaInterna(Boolean notaInterna) { this.notaInterna = notaInterna; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}