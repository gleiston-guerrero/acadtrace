package ec.edu.uteq.sga.dto.notificacion;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificacionResponseDTO {
    private Long idNotificacion;
    private String tipo;
    private String titulo;
    private String mensaje;
    private String urlDestino;
    private boolean leida;
    private Instant fecha;
}
