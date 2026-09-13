package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.TraceContext;
import ec.uteq.sga.secretaria.infrastructure.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.infrastructure.security.HmacService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Escritor de Secretaria sobre la bitacora institucional.
 *
 * M2/M3 utilizan la misma cabeza persistida que sga-principal y bloquean
 * id_estado=1 mediante SELECT ... FOR UPDATE antes de calcular el siguiente
 * eslabon. De esta forma dos escritores concurrentes no pueden producir
 * bifurcaciones de la cadena.
 */
@Service
public class AuditoriaService {

    private static final Logger log =
            LoggerFactory.getLogger(AuditoriaService.class);

    private static final String SCHEMA_ORIGEN = "SECRETARIA";

    private final NamedParameterJdbcTemplate jdbc;
    private final HmacService hmacService;
    private final LamportClock lamportClock;
    private final AuditHashService auditHashService;

    @Value("${AUDIT:m2}")
    private String auditMode = "m2";

    public AuditoriaService(
            NamedParameterJdbcTemplate jdbc,
            HmacService hmacService
    ) {
        this(
                jdbc,
                hmacService,
                new LamportClock(),
                new AuditHashService()
        );
    }

    @Autowired
    public AuditoriaService(
            NamedParameterJdbcTemplate jdbc,
            HmacService hmacService,
            LamportClock lamportClock,
            AuditHashService auditHashService
    ) {
        this.jdbc = jdbc;
        this.hmacService = hmacService;
        this.lamportClock =
                lamportClock != null
                        ? lamportClock
                        : new LamportClock();
        this.auditHashService =
                auditHashService != null
                        ? auditHashService
                        : new AuditHashService();
    }

    public void setAuditMode(String mode) {
        this.auditMode = mode;
    }

    public String getAuditMode() {
        return auditMode;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarCrud(
            String accion,
            String tablaAfectada,
            Long registroId,
            String descripcion
    ) {
        guardar(
                accion,
                tablaAfectada,
                registroId,
                descripcion,
                "EXITO"
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarFallo(
            String accion,
            String tablaAfectada,
            Long registroId,
            String descripcion
    ) {
        guardar(
                accion,
                tablaAfectada,
                registroId,
                descripcion,
                "FALLO"
        );
    }

    private void guardar(
            String accion,
            String tablaAfectada,
            Long registroId,
            String descripcion,
            String resultado
    ) {
        if ("m0".equalsIgnoreCase(auditMode)) {
            return;
        }

        try {
            String username = usernameActual();
            UUID traceUuid =
                    parseOrNew(TraceContext.current());

            Instant fecha =
                    Instant.now()
                            .truncatedTo(ChronoUnit.MILLIS);

            String ip = ipActual();

            /*
             * M1 conserva la bitacora relacional convencional.
             */
            if ("m1".equalsIgnoreCase(auditMode)) {
                MapSqlParameterSource params =
                        new MapSqlParameterSource()
                                .addValue("username", username)
                                .addValue("accion", accion)
                                .addValue(
                                        "tablaAfectada",
                                        tablaAfectada
                                )
                                .addValue(
                                        "registroId",
                                        registroId
                                )
                                .addValue(
                                        "descripcion",
                                        descripcion
                                )
                                .addValue("ip", ip)
                                .addValue(
                                        "traceId",
                                        traceUuid.toString()
                                )
                                .addValue(
                                        "resultado",
                                        resultado
                                )
                                .addValue(
                                        "fecha",
                                        fecha.atOffset(
                                                ZoneOffset.UTC
                                        )
                                );

                jdbc.update("""
                        INSERT INTO sga_principal.auditoria
                            (
                                schema_origen,
                                username,
                                accion,
                                tabla_afectada,
                                registro_id,
                                descripcion,
                                ip_address,
                                trace_id,
                                resultado,
                                fecha
                            )
                        VALUES
                            (
                                'SECRETARIA',
                                :username,
                                CAST(
                                    :accion
                                    AS sga_principal.accion_auditoria_t
                                ),
                                :tablaAfectada,
                                :registroId,
                                :descripcion,
                                :ip,
                                CAST(:traceId AS uuid),
                                :resultado,
                                :fecha
                            )
                        """, params);

                return;
            }

            /*
             * La lectura FOR UPDATE mantiene bloqueada la cabeza hasta que
             * esta transaccion inserta el evento y confirma la nueva cabeza.
             */
            Map<String, Object> estado = jdbc.queryForMap("""
                    SELECT
                        ultimo_hash,
                        ultimo_lamport,
                        vector_reloj
                    FROM sga_principal.estado_cadena_auditoria
                    WHERE id_estado = 1
                    FOR UPDATE
                    """, new MapSqlParameterSource());

            String hashAnterior =
                    estado.get("ultimo_hash") != null
                            ? String.valueOf(
                                    estado.get("ultimo_hash")
                            )
                            : AuditHashService.GENESIS_HASH;

            long ultimoLamport =
                    estado.get("ultimo_lamport")
                            instanceof Number numero
                            ? numero.longValue()
                            : 0L;

            long lamport = ultimoLamport + 1L;

            /*
             * El contador en memoria es solo espejo; la autoridad es la BD.
             */
            lamportClock.seed(lamport);

            Map<String, Long> vector = null;
            String vectorJson = null;

            if ("m3".equalsIgnoreCase(auditMode)) {
                vector = auditHashService.parseVector(
                        estado.get("vector_reloj") != null
                                ? String.valueOf(
                                        estado.get("vector_reloj")
                                )
                                : "{}"
                );

                vector.put(
                        "secretaria",
                        vector.getOrDefault(
                                "secretaria",
                                0L
                        ) + 1L
                );

                vectorJson =
                        auditHashService.vectorJson(vector);
            }

            Map<String, Object> payload =
                    new TreeMap<>();

            payload.put("descripcion", descripcion);
            payload.put("ip_address", ip);
            payload.put("resultado", resultado);
            payload.put(
                    "schema_origen",
                    SCHEMA_ORIGEN
            );
            payload.put(
                    "trace_id",
                    traceUuid.toString()
            );

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
                            lamport,
                            vector,
                            estadoReconciliacion
                    );

            String canonico =
                    auditHashService.jsonCanonico(
                            contenido
                    );

            String hashActual =
                    auditHashService.calcularHash(
                            hashAnterior,
                            contenido
                    );

            String hmac = hmacService.firmar(
                    SCHEMA_ORIGEN,
                    String.valueOf(traceUuid),
                    nvl(username),
                    accion,
                    nvl(tablaAfectada),
                    String.valueOf(registroId),
                    nvl(descripcion),
                    resultado,
                    String.valueOf(
                            fecha.toEpochMilli()
                    )
            );

            MapSqlParameterSource params =
                    new MapSqlParameterSource()
                            .addValue("username", username)
                            .addValue("accion", accion)
                            .addValue(
                                    "tablaAfectada",
                                    tablaAfectada
                            )
                            .addValue(
                                    "registroId",
                                    registroId
                            )
                            .addValue(
                                    "descripcion",
                                    descripcion
                            )
                            .addValue("ip", ip)
                            .addValue(
                                    "traceId",
                                    traceUuid.toString()
                            )
                            .addValue(
                                    "resultado",
                                    resultado
                            )
                            .addValue("hmac", hmac)
                            .addValue(
                                    "hashAnterior",
                                    hashAnterior
                            )
                            .addValue(
                                    "hashActual",
                                    hashActual
                            )
                            .addValue(
                                    "relojLamport",
                                    lamport
                            )
                            .addValue(
                                    "vectorReloj",
                                    vectorJson
                            )
                            .addValue(
                                    "contenidoCanonico",
                                    canonico
                            )
                            .addValue(
                                    "versionCanonica",
                                    "v1"
                            )
                            .addValue(
                                    "fecha",
                                    fecha.atOffset(
                                            ZoneOffset.UTC
                                    )
                            );

            jdbc.update("""
                    INSERT INTO sga_principal.auditoria
                        (
                            schema_origen,
                            username,
                            accion,
                            tabla_afectada,
                            registro_id,
                            descripcion,
                            ip_address,
                            trace_id,
                            resultado,
                            hmac,
                            hash_anterior,
                            hash_actual,
                            reloj_lamport,
                            vector_reloj,
                            contenido_canonico,
                            version_canonica,
                            fecha
                        )
                    VALUES
                        (
                            'SECRETARIA',
                            :username,
                            CAST(
                                :accion
                                AS sga_principal.accion_auditoria_t
                            ),
                            :tablaAfectada,
                            :registroId,
                            :descripcion,
                            :ip,
                            CAST(:traceId AS uuid),
                            :resultado,
                            :hmac,
                            :hashAnterior,
                            :hashActual,
                            :relojLamport,
                            :vectorReloj,
                            :contenidoCanonico,
                            :versionCanonica,
                            :fecha
                        )
                    """, params);

            MapSqlParameterSource estadoParams =
                    new MapSqlParameterSource()
                            .addValue(
                                    "hashActual",
                                    hashActual
                            )
                            .addValue(
                                    "relojLamport",
                                    lamport
                            )
                            .addValue(
                                    "vectorReloj",
                                    vectorJson
                            );

            jdbc.update("""
                    UPDATE sga_principal.estado_cadena_auditoria
                    SET
                        ultimo_hash = :hashActual,
                        ultimo_lamport = :relojLamport,
                        vector_reloj = COALESCE(CAST(:vectorReloj AS TEXT), vector_reloj)
                    WHERE id_estado = 1
                    """, estadoParams);

        } catch (Exception e) {
            /*
             * No dejamos confirmar media operacion de auditoria:
             * evento y cabeza se revierten juntos.
             */
            try {
                TransactionAspectSupport
                        .currentTransactionStatus()
                        .setRollbackOnly();
            } catch (Exception ignored) {
                // Pruebas unitarias sin proxy transaccional.
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

    private static String usernameActual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes)
                            RequestContextHolder
                                    .currentRequestAttributes();

            HttpServletRequest request =
                    attrs.getRequest();

            AuthenticatedUser user =
                    (AuthenticatedUser)
                            request.getAttribute(
                                    AuthenticatedUser
                                            .REQUEST_ATTRIBUTE
                            );

            return user != null
                    ? user.username()
                    : null;

        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static String ipActual() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes)
                            RequestContextHolder
                                    .currentRequestAttributes();

            return attrs
                    .getRequest()
                    .getRemoteAddr();

        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static UUID parseOrNew(String value) {
        if (value == null) {
            return UUID.randomUUID();
        }

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }
}
