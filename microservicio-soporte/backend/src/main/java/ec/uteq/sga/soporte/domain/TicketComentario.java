package ec.uteq.sga.soporte.domain;

import java.time.LocalDateTime;

/** Entidad de dominio pura para un comentario de ticket. Sin anotaciones de framework. */
public class TicketComentario {

    private Long id;
    private Long idTicket;
    private String autor;
    private String contenido;
    private Boolean notaInterna;
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
