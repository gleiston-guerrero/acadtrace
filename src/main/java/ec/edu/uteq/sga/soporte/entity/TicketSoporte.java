package ec.edu.uteq.sga.soporte.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "tickets", schema = "soporte")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketSoporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_ticket")
    private Long idTicket;

    @Column(name = "numero_ticket", unique = true, nullable = false, length = 20)
    private String numeroTicket;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String prioridad = "MEDIO";

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "ABIERTO";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String categoria = "OTRO";

    @Column(name = "creado_por", nullable = false, length = 50)
    private String creadoPor;

    @Column(name = "asignado_a", length = 50)
    private String asignadoA;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    @Builder.Default
    private Instant fechaCreacion = Instant.now();

    @Column(name = "fecha_actualizacion")
    @Builder.Default
    private Instant fechaActualizacion = Instant.now();

    @Column(name = "fecha_resolucion")
    private Instant fechaResolucion;

    @Column(name = "solucion_aplicada", columnDefinition = "text")
    private String solucionAplicada;
}