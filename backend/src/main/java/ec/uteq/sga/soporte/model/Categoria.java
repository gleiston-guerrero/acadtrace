package ec.uteq.sga.soporte.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("categorias")
public class Categoria {

    @Id
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;

    public Categoria() {}

    public Categoria(Long id, String nombre, String descripcion, Boolean activo) {
        this.id = id;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.activo = activo;
    }

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}