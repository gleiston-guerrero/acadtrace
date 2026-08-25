package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "notificaciones", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(columnDefinition = "text")
    private String mensaje;

    @Column(name = "url_destino", length = 255)
    private String urlDestino;

    @Builder.Default
    @Column(nullable = false)
    private boolean leida = false;

    @Builder.Default
    @Column(nullable = false)
    private Instant fecha = Instant.now();
}
