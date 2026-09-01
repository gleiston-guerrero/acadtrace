package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalTime;

@Entity
@Table(name = "periodos_horario", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PeriodoHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_periodo")
    private Integer idPeriodo;

    @Column(nullable = false, length = 30)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false)
    private Boolean activo = true;
}
