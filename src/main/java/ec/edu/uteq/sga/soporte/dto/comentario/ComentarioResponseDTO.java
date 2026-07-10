package ec.edu.uteq.sga.soporte.dto.comentario;

import lombok.*;
import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComentarioResponseDTO {
    private Long idComentario;
    private Long idTicket;
    private String contenido;
    private String autor;
    private boolean notaInterna;
    private Instant fechaCreacion;
}