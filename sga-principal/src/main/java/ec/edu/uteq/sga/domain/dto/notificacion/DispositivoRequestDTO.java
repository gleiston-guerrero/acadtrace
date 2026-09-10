package ec.edu.uteq.sga.domain.dto.notificacion;
import jakarta.validation.constraints.*;
public record DispositivoRequestDTO(@NotBlank @Size(max=512) String token, @NotBlank @Pattern(regexp="ANDROID") String plataforma) {}
