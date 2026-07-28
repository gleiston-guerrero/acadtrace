package ec.uteq.sga.soporte.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("tickets")
public class Ticket {

    @Id
    private Long id;
    private String codigo;
    private String titulo;
    private String descripcion;
    private String estado;
    private String prioridad;

    @Column("usuario_solicitante_id")
    private Long usuarioSolicitanteId;

    @Column("tecnico_asignado_id")
    private Long tecnicoAsignadoId;

    @Column("categoria_id")
    private Long categoriaId;

    @Column("fecha_creacion")
    private LocalDateTime fechaCreacion;

    @Column("fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column("fecha_cierre")
    private LocalDateTime fechaCierre;

    public Ticket() {
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
        this.estado = EstadoTicket.PENDIENTE.name();
        this.prioridad = PrioridadTicket.MEDIA.name();
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String prioridad) { this.prioridad = prioridad; }

    public Long getUsuarioSolicitanteId() { return usuarioSolicitanteId; }
    public void setUsuarioSolicitanteId(Long usuarioSolicitanteId) { this.usuarioSolicitanteId = usuarioSolicitanteId; }

    public Long getTecnicoAsignadoId() { return tecnicoAsignadoId; }
    public void setTecnicoAsignadoId(Long tecnicoAsignadoId) { this.tecnicoAsignadoId = tecnicoAsignadoId; }

    public Long getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Long categoriaId) { this.categoriaId = categoriaId; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime fechaActualizacion) { this.fechaActualizacion = fechaActualizacion; }

    public LocalDateTime getFechaCierre() { return fechaCierre; }
    public void setFechaCierre(LocalDateTime fechaCierre) { this.fechaCierre = fechaCierre; }
}