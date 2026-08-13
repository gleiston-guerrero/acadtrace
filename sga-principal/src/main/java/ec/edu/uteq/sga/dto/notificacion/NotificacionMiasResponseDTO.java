package ec.edu.uteq.sga.dto.notificacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificacionMiasResponseDTO {
    private List<NotificacionResponseDTO> notificaciones;
    private long noLeidas;
}
