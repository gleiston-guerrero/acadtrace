package ec.edu.uteq.sga.domain.entity.dto.notificacion;

import lombok.*;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificacionMiasResponseDTO {
    private List<NotificacionResponseDTO> notificaciones;
    private long noLeidas;
}
