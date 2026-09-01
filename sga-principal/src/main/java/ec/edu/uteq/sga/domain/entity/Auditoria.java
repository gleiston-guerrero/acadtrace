package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnTransformer;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoria centralizado (originado en FIX-3 para PRINCIPAL y
 * DOCENTE, extendido en V8 para incluir microservicio-secretaria).
 * traceId correlaciona un mismo evento de negocio entre microservicios
 * (ver TraceIdFilter / InternalAuthInterceptor); hmac firma los campos
 * clave de la fila para detectar alteraciones hechas por fuera de la
 * aplicacion (ver AuditoriaService).
 */
@Entity
@Table(name = "auditoria", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_auditoria")
    private Long idAuditoria;

    @Builder.Default
    @Column(name = "schema_origen", nullable = false, length = 20)
    private String schemaOrigen = "PRINCIPAL";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private Usuario usuario;

    @Column(length = 50)
    private String username;

    /**
     * accion es un enum nativo de Postgres (accion_auditoria_t), no varchar.
     * ColumnTransformer castea el bind parameter en el INSERT/UPDATE y en
     * cualquier comparacion JPQL (a.accion = :accion); sin esto Postgres
     * rechaza el parametro por venir tipado como "unknown"/varchar.
     */
    @Column(nullable = false, length = 50)
    @ColumnTransformer(write = "?::sga_principal.accion_auditoria_t")
    private String accion;

    @Column(name = "tabla_afectada", length = 50)
    private String tablaAfectada;

    @Column(name = "registro_id")
    private Long registroId;

    @Column(columnDefinition = "text")
    private String descripcion;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;

    @Column(name = "trace_id", nullable = false)
    private UUID traceId;

    @Builder.Default
    @Column(nullable = false, length = 10)
    private String resultado = "EXITO";

    @Column(length = 64)
    private String hmac;

    @Builder.Default
    @Column(nullable = false)
    private Instant fecha = Instant.now();
}