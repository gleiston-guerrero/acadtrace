package ec.edu.uteq.sga.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RestriccionBitacoraValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RestriccionBitacoraValidator.class);
    private final JdbcTemplate jdbcTemplate;

    public RestriccionBitacoraValidator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Validando restricciones de seguridad sobre la bitácora de auditoría (Punto 45)...");
        try {
            // Intentamos un UPDATE a la bitácora, lo cual debe ser rechazado por falta de permisos.
            jdbcTemplate.update("UPDATE sga_principal.auditoria SET accion = 'MODIFICAR' WHERE 1=0");

            // Si pasa silenciosamente sin error de permisos, significa que somos superusuarios o dueños de la tabla.
            log.error("VULNERABILIDAD CRÍTICA: El usuario actual tiene permisos de modificación sobre la bitácora.");
            throw new IllegalStateException("Arranque abortado: El usuario conectado no cumple con la restricción de privilegios sobre la bitácora. Se requiere usar el rol limitado (sga_app).");
            
        } catch (DataAccessException e) {
            // Buscamos el código 42501 (Insufficient Privilege) u otro indicador de rechazo
            if (e.getMessage() != null && e.getMessage().contains("42501")) {
                log.info("Seguridad de bitácora confirmada: El usuario actual NO tiene permisos de modificación (Error 42501).");
            } else {
                log.info("La base de datos bloqueó la operación exitosamente: {}", e.getMessage());
            }
        }
    }
}

