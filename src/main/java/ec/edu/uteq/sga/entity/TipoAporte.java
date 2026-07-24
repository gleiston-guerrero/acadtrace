package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Actividad evaluable con la que el docente registra notas. Las de tipo
 * FORMATIVA promedian la evaluación formativa (lección oral, tareas, ...);
 * las SUMATIVA son el proyecto interdisciplinario y el examen del trimestre.
 */
@Entity
@Table(name = "tipos_aporte", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TipoAporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_aporte")
    private Long idTipoAporte;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ano_lectivo", nullable = false)
    private AnoLectivo anoLectivo;

    @Column(nullable = false, length = 60)
    private String nombre;

    @Column(name = "tipo_evaluacion", nullable = false, length = 12)
    private String tipoEvaluacion; // FORMATIVA o SUMATIVA

    @Column(nullable = false)
    private Integer orden;

    @Builder.Default
    @Column(nullable = false)
    private boolean activo = true;
}
