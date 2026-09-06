package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.auditoria.AuditoriaResponseDTO;
import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.security.HmacService;
import ec.edu.uteq.sga.infrastructure.web.TraceContext;
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

import org.springframework.beans.factory.annotation.Value;

/**
 * Unico punto de escritura de sga_principal.auditoria desde sga-principal
 * con soporte de conmutacion en caliente por variable AUDIT (m0, m1, m2, m3).
 */
@Service
@RequiredArgsConstructor
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository repo;
    private final HmacService hmacService;
    private final LamportClock lamportClock;

    @Value("${AUDIT:m2}")
    private String auditMode = "m2";

    public void setAuditMode(String mode) {
        this.auditMode = mode;
    }

    public String getAuditMode() {
        return this.auditMode;
    }

    public void registrarCrud(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    public void registrarConfig(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    public void registrarAuth(String accion, String username, Long idUsuario, String resultado, String descripcion) {
        guardar(accion, "usuario", idUsuario, descripcion, username, null, resultado, ipActual());
    }

    /** Llamada gRPC recibida desde otro microservicio. */
    public void registrarGrpcRecibida(String tablaAfectada, Long registroId, String descripcion, String resultado, String mensajeError) {
        String desc = mensajeError != null ? descripcion + " — " + mensajeError : descripcion;
        guardar("LLAMADA_GRPC", tablaAfectada, registroId, desc, TraceContext.actor(), TraceContext.current(), resultado, null);
    }

    public void registrarFalloGrpcInterno(String descripcion) {
        guardar("LLAMADA_GRPC", null, null, descripcion, null, TraceContext.current(), "FALLO", null);
    }

    private void guardar(String accion, String tablaAfectada, Long registroId, String descripcion,
                          String username, String traceIdOverride, String resultado, String ip) {
        if ("m0".equalsIgnoreCase(auditMode)) {
            // Modo m0: Sin auditoria (linea base de desempeno)
            return;
        }

        try {
            UUID traceId = parseOrNew(traceIdOverride != null ? traceIdOverride : TraceContext.current());
            long lamportTime = (lamportClock != null) ? lamportClock.tick() : 1L;

            String descFinal = descripcion;
            if ("m2".equalsIgnoreCase(auditMode) || "m3".equalsIgnoreCase(auditMode)) {
                String extra = " [lamport:" + lamportTime + ("m3".equalsIgnoreCase(auditMode) ? ",vclock:[1,0,0]" : "") + "]";
                descFinal = (descripcion != null ? descripcion : "") + extra;
            }

            Auditoria fila = Auditoria.builder()
                    .schemaOrigen("PRINCIPAL")
                    .traceId(traceId)
                    .username(username)
                    .accion(accion)
                    .tablaAfectada(tablaAfectada)
                    .registroId(registroId)
                    .descripcion(descFinal)
                    .ipAddress(ip)
                    .resultado(resultado)
                    .fecha(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS))
                    .build();

            if (!"m1".equalsIgnoreCase(auditMode)) {
                // m1 es bitacora convencional; m2 y m3 llevan HMAC criptografico
                fila.setHmac(firmar(fila));
            }
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
