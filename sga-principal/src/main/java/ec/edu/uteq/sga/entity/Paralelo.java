package ec.edu.uteq.sga.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "paralelos", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Paralelo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paralelo")
    private Long idParalelo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_grado", nullable = false)
    private Grado grado;

    @Column(nullable = false, length = 1)
    private String letra;

    @Column
    private boolean activo = true;
}
