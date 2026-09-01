package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "malla_curricular", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MallaCurricular {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_malla")
    private Long idMalla;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado", nullable = false)
    private Grado grado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_asignatura", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_lectivo", nullable = false)
    private AnoLectivo anoLectivo;

    @Column(name = "horas_semana", nullable = false)
    private Short horasSemana;

    @Column(name = "dias_semana")
    private Short diasSemana;

    private Short duracion;

    @Builder.Default
    private boolean activo = true;

    @Builder.Default
    @Column(name = "fecha_creacion")
    private Instant fechaCreacion = Instant.now();
}
