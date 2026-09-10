package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="dispositivos_representante", schema="sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DispositivoRepresentante {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="id_dispositivo") private Long idDispositivo;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="id_usuario", nullable=false) private Usuario usuario;
    @Column(nullable=false, unique=true, length=512) private String token;
    @Column(nullable=false, length=20) private String plataforma;
    @Builder.Default @Column(nullable=false) private boolean activo=true;
    @Builder.Default @Column(name="fecha_registro", nullable=false) private Instant fechaRegistro=Instant.now();
    @Builder.Default @Column(name="fecha_actualizacion", nullable=false) private Instant fechaActualizacion=Instant.now();
}
