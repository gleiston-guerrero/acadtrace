package ec.edu.uteq.sga.domain.dto.usuario;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CambioPasswordDTO {

    @NotBlank
    private String passwordActual;

    @NotBlank
    @Size(min = 6, message = "Mínimo 6 caracteres")
    private String passwordNuevo;
}