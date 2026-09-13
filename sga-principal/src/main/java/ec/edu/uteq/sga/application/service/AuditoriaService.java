package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.auditoria.AuditoriaResponseDTO;
import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.domain.entity.EstadoCadenaAuditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.repository.EstadoCadenaAuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.security.HmacService;
import ec.edu.uteq.sga.infrastructure.web.TraceContext;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
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
    private final EstadoCadenaAuditoriaRepository estadoCadenaRepo;
    private final HmacService hmacService;
    private final LamportClock lamportClock;
    private final VectorClock vectorClock;
    private final AuditHashService auditHashService;

    @Value("${AUDIT:m2}")
    private String auditMode = "m2";

    public void setAuditMode(String mode) {
        this.auditMode = mode;
    }

    public String getAuditMode() {
        return this.auditMode;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarCrud(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConfig(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, usernameActual(), null, "EXITO", ipActual());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAuth(String accion, String username, Long idUsuario, String resultado, String descripcion) {
        guardar(accion, "usuario", idUsuario, descripcion, username, null, resultado, ipActual());
    }

    /** Llamada gRPC recibida desde otro microservicio. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarGrpcRecibida(String tablaAfectada, Long registroId, String descripcion, String resultado, String mensajeError) {
        String desc = mensajeError != null ? descripcion + " — " + mensajeError : descripcion;
        guardar("LLAMADA_GRPC", tablaAfectada, registroId, desc, TraceContext.actor(), TraceContext.current(), resultado, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
            Instant fecha = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);

            Auditoria fila = Auditoria.builder()
                    .schemaOrigen("PRINCIPAL")
                    .traceId(traceId)
                    .username(username)
                    .accion(accion)
                    .tablaAfectada(tablaAfectada)
                    .registroId(registroId)
                    .ipAddress(ip)
                    .resultado(resultado)
                    .fecha(fecha)
                    .build();

            if ("m1".equalsIgnoreCase(auditMode)) {
                // Modo m1: bitacora relacional convencional.
                fila.setDescripcion(descripcion);
                repo.save(fila);
                return;
            }

            /*
             * Modos m2/m3.
             *
             * La fila singleton se bloquea con PESSIMISTIC_WRITE durante
             * toda la transaccion. La BD, y no un contador en memoria, es
             * la autoridad de la cabeza y del reloj Lamport.
             */
            EstadoCadenaAuditoria estado = estadoCadenaRepo
                    .buscarParaActualizar((short) 1)
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe la cabeza de auditoria id_estado=1"
                    ));

            String hashAnterior = estado.getUltimoHash();
            if (hashAnterior == null || hashAnterior.isBlank()) {
                hashAnterior = AuditHashService.GENESIS_HASH;
            }

            long ultimoLamport = estado.getUltimoLamport() != null
                    ? estado.getUltimoLamport()
                    : 0L;
            long lamportTime = ultimoLamport + 1L;

            // Mantener sincronizado el reloj local, pero el valor oficial
            // procede siempre del estado persistido y bloqueado.
            if (lamportClock != null) {
                lamportClock.seed(lamportTime);
            }
            fila.setRelojLamport(lamportTime);

            Map<String, Long> vclock = null;
            String vclockJson = null;

            if ("m3".equalsIgnoreCase(auditMode) && vectorClock != null) {
                vectorClock.mergeJson(estado.getVectorReloj());
                vclock = vectorClock.increment("principal");
                vclockJson = vectorClock.toJson();
                fila.setVectorReloj(vclockJson);
            }

            fila.setDescripcion(descripcion);
            fila.setHashAnterior(hashAnterior);

            /*
             * Datos especificos de Principal viajan dentro de payload.
             * La envoltura exterior queda identica a la de Docente.
             */
            Map<String, Object> payload = new TreeMap<>();
            payload.put("descripcion", descripcion);
            payload.put("ip_address", ip);
            payload.put("resultado", resultado);
            payload.put("schema_origen", "PRINCIPAL");
            payload.put("trace_id", traceId.toString());

            String estadoReconciliacion =
                    "m3".equalsIgnoreCase(auditMode)
                            ? "APLICADO"
                            : "NO_APLICA";

            Map<String, Object> contenido =
                    auditHashService.contenidoEvento(
                            "AUDITORIA",
                            tablaAfectada,
                            registroId,
                            accion,
                            username,
                            fecha.toString(),
                            payload,
                            auditMode.toLowerCase(),
                            lamportTime,
                            vclock,
                            estadoReconciliacion
                    );

            String canonico = auditHashService.jsonCanonico(contenido);
            String hashActual = auditHashService.calcularHash(
                    hashAnterior,
                    contenido
            );

            fila.setContenidoCanonico(canonico);
            fila.setVersionCanonica("v1");
            fila.setHashActual(hashActual);

            // HMAC continua siendo una proteccion complementaria.
            fila.setHmac(firmar(fila));

            /*
             * INSERT del evento y avance de la cabeza ocurren en la misma
             * transaccion mientras la fila singleton sigue bloqueada.
             */
            repo.save(fila);

            estado.setUltimoHash(hashActual);
            estado.setUltimoLamport(lamportTime);
            if (vclockJson != null) {
                estado.setVectorReloj(vclockJson);
            }
            estadoCadenaRepo.save(estado);
        } catch (Exception e) {
            /*
             * Si algo falla despues de insertar el evento o mover la cabeza,
             * toda la transaccion de auditoria debe revertirse para no dejar
             * una cadena partida. El fallo sigue siendo fail-open respecto
             * de la operacion de negocio que origino la auditoria.
             */
            try {
                TransactionAspectSupport
                        .currentTransactionStatus()
                        .setRollbackOnly();
            } catch (Exception ignored) {
                // Permite pruebas unitarias sin proxy transaccional.
            }

            log.error(
                    "No se pudo registrar evento de auditoria ({} / {}): {}",
                    accion,
                    tablaAfectada,
                    e.getMessage(),
                    e
            );
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
