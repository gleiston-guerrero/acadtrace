package ec.edu.uteq.sga.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayConfig.class);

    /**
     * V18 se edito despues de aplicada para sustituir la contrasena literal
     * por el marcador ${sga_app_password}. Esa edicion altera el checksum del
     * archivo y hace fallar la validacion de Flyway con "checksum mismatch"
     * en cualquier base ya migrada con la version del 13/09.
     *
     * Este bean ejecuta flyway.repair() antes de flyway.migrate(): repair()
     * recalcula el checksum de todas las migraciones aplicadas contra el
     * archivo actual en el arbol, sin cambiar el esquema. A partir de ahi
     * migrate() aplica V19, V20 y siguientes con normalidad.
     *
     * En una base nueva repair() es un no-op (no hay historial que reparar),
     * por lo que este bean es seguro en ambos escenarios.
     */
    @Bean
    public FlywayMigrationStrategy repairAntesDeMigrar() {
        return flyway -> {
            log.info("Ejecutando flyway.repair() antes del migrate para recalcular checksums");
            flyway.repair();
            log.info("flyway.repair() completado, procediendo con migrate");
            flyway.migrate();
        };
    }
}
