package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "personas", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long idPersona;

    @OneToOne
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(columnDefinition = "sga_principal.genero_t")
    @ColumnTransformer(write = "?::sga_principal.genero_t")
    private String genero;

    @Column(length = 20)
    private String telefono;

    @Column(name = "telefono_alt", length = 20)
    private String telefonoAlt;

    @Column(columnDefinition = "text")
    private String direccion;

    @Column(name = "correo_personal", length = 100)
    private String correoPersonal;

    @Column(name = "titulo_academico", length = 100)
    private String tituloAcademico;

    @Column(length = 100)
    private String especializacion;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Builder.Default
    @Column(name = "fecha_creacion")
    private Instant fechaCreacion = Instant.now();

    @Builder.Default
    @Column(name = "fecha_actualizacion")
    private Instant fechaActualizacion = Instant.now();
}