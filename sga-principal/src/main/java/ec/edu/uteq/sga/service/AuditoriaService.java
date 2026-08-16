package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.auditoria.AuditoriaResponseDTO;
import ec.edu.uteq.sga.entity.Auditoria;
import ec.edu.uteq.sga.repository.AuditoriaRepository;
import ec.edu.uteq.sga.security.HmacService;
import ec.edu.uteq.sga.web.TraceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.UUID;

/**
 * Unico punto de escritura de sga_principal.auditoria desde sga-principal
 * (la tabla y el repositorio existian desde FIX-3, pero nada los llamaba).
 * microservicio-secretaria tiene su propio AuditoriaService (JDBC) que
 * escribe en la misma tabla via SQL cruzado, ya que ambos comparten la
 * misma base de datos fisica; comparten tambien el mismo secreto HMAC
 * (jwt.secret) para que las firmas sean verificables sin importar cual
 * servicio escribio la fila.
 *
 * Nunca deja que un fallo al auditar tumbe la operacion de negocio que
 * disparo el evento: se loguea el error y se continua.
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository repo;
    private final HmacService hmacService;

    public void registrarCrud(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    public void registrarConfig(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    public void registrarAuth(String accion, String username, Long idUsuario, String resultado, String descripcion) {
        guardar(accion, "usuario", idUsuario, descripcion, username, null, resultado, ipActual());
    }

    /** Llamada gRPC recibida desde otro microservicio (ver InternalAuthInterceptor, que puebla TraceContext con el trace_id/actor que trae la metadata). */
    public void registrarGrpcRecibida(String tablaAfectada, Long registroId, String descripcion, String resultado, String mensajeError) {
        String desc = mensajeError != null ? descripcion + " — " + mensajeError : descripcion;
        guardar("LLAMADA_GRPC", tablaAfectada, registroId, desc, TraceContext.actor(), TraceContext.current(), resultado, null);
    }

    public void registrarFalloGrpcInterno(String descripcion) {
        guardar("LLAMADA_GRPC", null, null, descripcion, null, TraceContext.current(), "FALLO", null);
    }

    private void guardar(String accion, String tablaAfectada, Long registroId, String descripcion,
                          String username, String traceIdOverride, String resultado, String ip) {
        try {
            UUID traceId = parseOrNew(traceIdOverride != null ? traceIdOverride : TraceContext.current());

            Auditoria fila = Auditoria.builder()
                    .schemaOrigen("PRINCIPAL")
                    .traceId(traceId)
                    .username(username)
                    .accion(accion)
                    .tablaAfectada(tablaAfectada)
                    .registroId(registroId)
                    .descripcion(descripcion)
                    .ipAddress(ip)
                    .resultado(resultado)
                    .fecha(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
                    .build();

            fila.setHmac(firmar(fila));
            repo.save(fila);
        } catch (Exception e) {
            // Un fallo al auditar no debe romper la operacion que lo disparo.
            log.error("No se pudo registrar evento de auditoria ({} / {}): {}", accion, tablaAfectada, e.getMessage(), e);
        }
    }

    private String firmar(Auditoria a) {
        return hmacService.firmar(
                a.getSchemaOrigen(),
                String.valueOf(a.getTraceId()),
                nvl(a.getUsername()),
                a.getAccion(),
                nvl(a.getTablaAfectada()),
                String.valueOf(a.getRegistroId()),
                nvl(a.getDescripcion()),
                a.getResultado(),
                String.valueOf(a.getFecha().toEpochMilli())
        );
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }

    private static UUID parseOrNew(String value) {
        if (value == null) return UUID.randomUUID();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static String usernameActual() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private static String ipActual() {
        try {
            var attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getRemoteAddr();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    // ---- Lectura (para AuditoriaController) ----

    public Page<AuditoriaResponseDTO> buscar(String schemaOrigen, String accion, String categoria, String tablaAfectada,
                                              String resultado, String username, Pageable pageable) {
        return repo.buscar(blankToNull(schemaOrigen), blankToNull(accion), blankToNull(categoria), blankToNull(tablaAfectada),
                        blankToNull(resultado), blankToNull(username), pageable)
                .map(this::toDTO);
    }

    public java.util.List<AuditoriaResponseDTO> porTrace(UUID traceId) {
        return repo.findByTraceIdOrderByFechaAsc(traceId).stream().map(this::toDTO).toList();
    }

    private AuditoriaResponseDTO toDTO(Auditoria a) {
        boolean hmacValido = a.getHmac() != null && a.getHmac().equals(firmar(a));
        return AuditoriaResponseDTO.builder()
                .idAuditoria(a.getIdAuditoria())
                .traceId(a.getTraceId())
                .schemaOrigen(a.getSchemaOrigen())
                .idUsuario(a.getUsuario() != null ? a.getUsuario().getIdUsuario() : null)
                .username(a.getUsername())
                .accion(a.getAccion())
                .tablaAfectada(a.getTablaAfectada())
                .registroId(a.getRegistroId())
                .descripcion(a.getDescripcion())
                .ipAddress(a.getIpAddress())
                .resultado(a.getResultado())
                .hmacValido(hmacValido)
                .fecha(a.getFecha())
                .build();
    }

    private static String blankToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }
}
