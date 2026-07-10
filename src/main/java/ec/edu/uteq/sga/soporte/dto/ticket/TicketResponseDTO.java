package ec.edu.uteq.sga.soporte.dto.ticket;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketResponseDTO {
    private Long idTicket;
    private String numeroTicket;
    private String titulo;
    private String descripcion;
    private String prioridad;
    private String estado;
    private String categoria;
    private String creadoPor;
    private String asignadoA;
    private String solucionAplicada;
    private Instant fechaCreacion;
    private Instant fechaActualizacion;
    private Instant fechaResolucion;
}