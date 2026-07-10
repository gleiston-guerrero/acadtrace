package ec.edu.uteq.sga.soporte.dto.comentario;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ComentarioRequestDTO {

    @NotBlank(message = "El contenido no puede estar vacío")
    private String contenido;

    private boolean notaInterna = false;
}