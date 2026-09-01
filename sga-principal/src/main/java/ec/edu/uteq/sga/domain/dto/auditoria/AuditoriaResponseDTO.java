package ec.edu.uteq.sga.domain.dto.auditoria;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditoriaResponseDTO {
    private Long idAuditoria;
    private UUID traceId;
    private String schemaOrigen;
    private Long idUsuario;
    private String username;
    private String accion;
    private String tablaAfectada;
    private Long registroId;
    private String descripcion;
    private String ipAddress;
    private String resultado;
    private boolean hmacValido;
    private Instant fecha;
}
