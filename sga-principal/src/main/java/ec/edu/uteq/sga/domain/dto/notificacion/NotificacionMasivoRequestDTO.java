package ec.edu.uteq.sga.domain.dto.notificacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import java.util.List;

/** Request de otros microservicios (docente/soporte) para notificar a varios usuarios a la vez. */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificacionMasivoRequestDTO {

    @NotEmpty(message = "Debe indicar al menos un id_usuario destinatario")
    private List<Long> idsUsuarios;

    @NotBlank(message = "El tipo es obligatorio")
    private String tipo;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String mensaje;

    private String urlDestino;
}
