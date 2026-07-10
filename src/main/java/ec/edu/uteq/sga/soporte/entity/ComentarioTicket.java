package ec.edu.uteq.sga.soporte.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "comentarios_ticket", schema = "soporte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComentarioTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario")
    private Long idComentario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_ticket", nullable = false)
    private TicketSoporte ticket;

    @Column(nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "autor", nullable = false, length = 50)
    private String autor;

    @Column(name = "nota_interna")
    @Builder.Default
    private boolean notaInterna = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();
}
