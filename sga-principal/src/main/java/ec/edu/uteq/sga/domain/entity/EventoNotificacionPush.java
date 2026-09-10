package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="eventos_notificacion_push", schema="sga_principal",
 uniqueConstraints=@UniqueConstraint(name="uq_evento_push_destinatario", columnNames={"clave_evento","id_usuario"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventoNotificacionPush {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_evento") private Long idEvento;
    @Column(name="clave_evento", nullable=false, length=180) private String claveEvento;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_usuario", nullable=false) private Usuario usuario;
    @Column(nullable=false, length=30) private String tipo;
    @Builder.Default @Column(nullable=false, length=20) private String estado="PENDIENTE";
    @Builder.Default @Column(name="fecha_creacion", nullable=false) private Instant fechaCreacion=Instant.now();
    @Column(name="fecha_envio") private Instant fechaEnvio;
}
