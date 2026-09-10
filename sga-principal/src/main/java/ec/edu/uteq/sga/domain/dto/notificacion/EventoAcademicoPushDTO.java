package ec.edu.uteq.sga.domain.dto.notificacion;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
public record EventoAcademicoPushDTO(
 @NotBlank String eventKey, @NotBlank @Pattern(regexp="COMUNICADO|AUSENTE|ATRASO") String type,
 Long matriculaId, Long asignacionId, Long periodId, Long announcementId,
 String title, String body, LocalDate date) {}
