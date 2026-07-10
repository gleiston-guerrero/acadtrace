package ec.edu.uteq.sga.soporte.dto.ticket;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketUpdateDTO {
    private String estado;
    private String asignadoA;
    private String solucionAplicada;
}
