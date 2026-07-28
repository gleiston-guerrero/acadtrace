package ec.uteq.sga.soporte.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Entidad Spring Data JDBC que mapea sga_soporte.tickets. NOTA: el flujo real
 * de la app (TicketController/TicketService) usa JdbcTemplate directo con
 * SQL propio y no pasa por esta clase; esta entidad queda disponible para
 * quien la quiera usar mas adelante, pero hoy no esta conectada a ningun
 * controller.
 */
@Table("sga_soporte.tickets")
public class Ticket {

    @Id
    @Column("id_ticket")
    private Long id;

    @Column("numero_ticket")
    private String numeroTicket;

    private String titulo;
    private String descripcion;
    private String categoria;
    private String prioridad;
    private String estado;

    @Column("creado_por")
    private String creadoPor;

    @Column("asignado_a")
    private String asignadoA;

    @Column("solucion_aplicada")
    private String solucionAplicada;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column("fecha_resolucion")
    private LocalDateTime fechaResolucion;

    public Ticket() {
        this.fechaCreacion = LocalDateTime.now();
        this.estado = EstadoTicket.ABIERTO.name();
        this.prioridad = PrioridadTicket.MEDIO.name();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumeroTicket() { return numeroTicket; }
    public void setNumeroTicket(String numeroTicket) { this.numeroTicket = numeroTicket; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getCategoria() { return categoria; }
    public void setCategoria(String categoria) { this.categoria = categoria; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor; }

    public String getAsignadoA() { return asignadoA; }
    public void setAsignadoA(String asignadoA) { this.asignadoA = asignadoA; }

    public String getSolucionAplicada() { return solucionAplicada; }
    public void setSolucionAplicada(String solucionAplicada) { this.solucionAplicada = solucionAplicada; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime fechaResolucion) { this.fechaResolucion = fechaResolucion; }
}