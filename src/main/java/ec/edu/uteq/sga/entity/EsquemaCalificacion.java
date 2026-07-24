package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Ponderación con la que se arma el promedio trimestral de cada asignatura:
 * evaluación formativa (aportes) + evaluación sumativa (proyecto y examen).
 * Por defecto 70% / 30%. Es configuración: el microservicio docente la consulta
 * y con ella calcula, pero las notas se guardan en sga_docente.
 */
@Entity
@Table(name = "esquema_calificacion", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EsquemaCalificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_esquema")
    private Long idEsquema;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_lectivo", nullable = false, unique = true)
    private AnoLectivo anoLectivo;

    @Column(name = "peso_formativa", nullable = false)
    private BigDecimal pesoFormativa;

    @Column(name = "peso_sumativa", nullable = false)
    private BigDecimal pesoSumativa;
}
