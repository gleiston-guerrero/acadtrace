package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "matriculas", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_matricula")
    private Long idMatricula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudiante", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_lectivo", nullable = false)
    private AnoLectivo anoLectivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paralelo")
    private Paralelo paralelo;

    @Column(name = "numero_orden")
    private Short numeroOrden;

    @Builder.Default
    @Column(name = "fecha_registro")
    private LocalDate fechaRegistro = LocalDate.now();

    @Builder.Default
    @Column(nullable = false, columnDefinition = "sga_principal.estado_matricula_t")
    @ColumnTransformer(write = "?::sga_principal.estado_matricula_t")
    private String estado = "ACTIVA";

    @Column(columnDefinition = "text")
    private String observaciones;

    @Builder.Default
    @Column(name = "fecha_creacion")
    private Instant fechaCreacion = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por")
    private Usuario registradoPor;
}