package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.TraceContext;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import ec.uteq.sga.secretaria.security.HmacService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Escribe en sga_principal.auditoria (tabla centralizada, ver
 * ec.edu.uteq.sga.service.AuditoriaService en sga-principal) via SQL
 * cruzado: secretaria ya consulta sga_principal.usuarios directamente
 * (EstudianteService), asi que insertar aqui con el mismo
 * NamedParameterJdbcTemplate no agrega ningun salto de red nuevo.
 *
 * Firma cada fila con el mismo HMAC-SHA256 (mismo secreto, mismo orden de
 * campos) que usa sga-principal, para que sea verificable sin importar
 * quien la escribio. Nunca deja que un fallo al auditar tumbe la
 * operacion de negocio que lo disparo.
 */
@Service
public class AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaService.class);
    private static final String SCHEMA_ORIGEN = "SECRETARIA";

    private final NamedParameterJdbcTemplate jdbc;
    private final HmacService hmacService;

    public AuditoriaService(NamedParameterJdbcTemplate jdbc, HmacService hmacService) {
        this.jdbc = jdbc;
        this.hmacService = hmacService;
    }

    public void registrarCrud(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, "EXITO");
    }

    public void registrarFallo(String accion, String tablaAfectada, Long registroId, String descripcion) {
        guardar(accion, tablaAfectada, registroId, descripcion, "FALLO");
    }

    private void guardar(String accion, String tablaAfectada, Long registroId, String descripcion, String resultado) {
        try {
            String username = usernameActual();
            String traceId = TraceContext.current();
            Instant fecha = Instant.now().truncatedTo(ChronoUnit.MILLIS);
            UUID traceUuid = parseOrNew(traceId);

            String hmac = hmacService.firmar(
                    SCHEMA_ORIGEN,
                    String.valueOf(traceUuid),
                    nvl(username),
                    accion,
                    nvl(tablaAfectada),
                    String.valueOf(registroId),
                    nvl(descripcion),
                    resultado,
                    String.valueOf(fecha.toEpochMilli())
            );

            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("username", username)
                    .addValue("accion", accion)
                    .addValue("tablaAfectada", tablaAfectada)
                    .addValue("registroId", registroId)
                    .addValue("descripcion", descripcion)
                    .addValue("ip", ipActual())
                    .addValue("traceId", traceUuid.toString())
                    .addValue("resultado", resultado)
                    .addValue("hmac", hmac)
                    // pgjdbc no infiere el tipo SQL para java.time.Instant via setObject;
                    // OffsetDateTime si tiene soporte nativo para timestamptz.
                    .addValue("fecha", fecha.atOffset(java.time.ZoneOffset.UTC));

            jdbc.update("""
                    INSERT INTO sga_principal.auditoria
                        (schema_origen, username, accion, tabla_afectada, registro_id, descripcion,
                         ip_address, trace_id, resultado, hmac, fecha)
                    VALUES
                        ('SECRETARIA', :username, CAST(:accion AS sga_principal.accion_auditoria_t), :tablaAfectada,
                         :registroId, :descripcion, :ip, CAST(:traceId AS uuid), :resultado, :hmac, :fecha)
                    """, params);
        } catch (Exception e) {
            log.error("No se pudo registrar evento de auditoria ({} / {}): {}", accion, tablaAfectada, e.getMessage(), e);
        }
    }

    private static String usernameActual() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            AuthenticatedUser user = (AuthenticatedUser) request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
            return user != null ? user.username() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static String ipActual() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attrs.getRequest().getRemoteAddr();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private static UUID parseOrNew(String value) {
        if (value == null) return UUID.randomUUID();
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    private static String nvl(String v) {
        return v == null ? "" : v;
    }
}
