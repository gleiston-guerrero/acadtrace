package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Rango de la escala cualitativa: convierte una nota cuantitativa (0-10) en su
 * equivalente cualitativo (A+, A-, B+, ... E-). Se define por año lectivo y
 * nivel educativo.
 */
@Entity
@Table(name = "escala_calificaciones", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EscalaCalificaciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_escala")
    private Long idEscala;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_lectivo", nullable = false)
    private AnoLectivo anoLectivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_nivel", nullable = false)
    private NivelEducativo nivel;

    @Column(name = "nota_minima", nullable = false)
    private BigDecimal notaMinima;

    @Column(name = "nota_maxima", nullable = false)
    private BigDecimal notaMaxima;

    @Column(name = "equivalente_cualitativo", length = 5)
    private String equivalenteCualitativo;

    @Column(length = 100)
    private String descripcion;
}
