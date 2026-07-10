package ec.edu.uteq.sga.soporte.dto.ticket;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketRequestDTO {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 150)
    private String titulo;

    @NotBlank(message = "La descripción es obligatoria")
    private String descripcion;

    @Pattern(regexp = "BAJO|MEDIO|ALTO|CRITICO", message = "Prioridad inválida")
    @Builder.Default
    private String prioridad = "MEDIO";

    @Pattern(regexp = "HARDWARE|SOFTWARE|RED|CUENTA|OTRO", message = "Categoría inválida")
    @Builder.Default
    private String categoria = "OTRO";
}
