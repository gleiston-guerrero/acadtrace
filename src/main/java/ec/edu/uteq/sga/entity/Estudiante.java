package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "estudiantes", schema = "public")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Estudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudiante")
    private Long idEstudiante;

    @Column(unique = true, length = 20)
    private String cedula;

    @Column(name = "codigo_estudiante", unique = true, length = 20)
    private String codigoEstudiante;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Column(length = 1)
    private String genero;

    @Column(columnDefinition = "text")
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(name = "telefono_principal", length = 20)
    private String telefonoPrincipal;

    @Column(length = 100)
    private String correo;

    @Column
    private boolean discapacidad = false;

    @Column(name = "tipo_discapacidad", length = 100)
    private String tipoDiscapacidad;

    @Column(name = "porcentaje_disc")
    private Short porcentajeDisc;

    @Column(name = "origen_listado", length = 50)
    private String origenListado = "DISTRITO";

    @Column(length = 20)
    private String estado = "ACTIVO";

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(name = "fecha_creacion")
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_representante")
    private Representante representante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;
}