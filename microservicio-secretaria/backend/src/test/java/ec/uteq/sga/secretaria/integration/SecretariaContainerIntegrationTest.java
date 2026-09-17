package ec.uteq.sga.secretaria.integration;

import ec.uteq.sga.secretaria.application.service.AuditoriaService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de Integracion en Contenedores para Secretaria (Criterio E13, E6 y E37).
 *
 * Valida:
 * 1. PostgreSQL limpio en contenedor Testcontainers efímero con migraciones Flyway.
 * 2. Persistencia de auditoría con formato canónico común (HMAC + Lamport) (E3).
 * 3. Trigger append-only que prohíbe terminantemente UPDATE y DELETE (E6).
 * 4. Restricción estricta de privilegios de usuario (E6): con 2 conexiones
 *    (administrador vs usuario de aplicación), demuestra que el usuario de aplicación
 *    NO puede actualizar ni borrar filas incluso si el trigger es desactivado temporalmente.
 * 5. Arranque limpio del backend y validación de endpoints Actuator.
 */
@SpringBootTest(properties = {
    "spring.flyway.enabled=true"
})
@AutoConfigureMockMvc
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SecretariaContainerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("sga")
            .withUsername("postgres")
            .withPassword("test_secret_pass")
            .withInitScript("db/init/baseline_flyway_v8.sql");

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        if (!postgres.isRunning()) {
            postgres.start();
        }
        System.setProperty("DB_HOST", postgres.getHost());
        System.setProperty("DB_PORT", String.valueOf(postgres.getFirstMappedPort()));
        System.setProperty("DB_NAME", postgres.getDatabaseName());
        System.setProperty("DB_USER", postgres.getUsername());
        System.setProperty("DB_PASSWORD", postgres.getPassword());
        System.setProperty("db.host", postgres.getHost());
        System.setProperty("db.port", String.valueOf(postgres.getFirstMappedPort()));
        System.setProperty("db.name", postgres.getDatabaseName());
        System.setProperty("db.user", postgres.getUsername());
        System.setProperty("db.password", postgres.getPassword());
        registry.add("db.host", postgres::getHost);
        registry.add("db.port", postgres::getFirstMappedPort);
        registry.add("db.name", postgres::getDatabaseName);
        registry.add("db.user", postgres::getUsername);
        registry.add("db.password", postgres::getPassword);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "8");
        registry.add("spring.flyway.schemas", () -> "sga_principal");
        registry.add("spring.flyway.default-schema", () -> "sga_principal");
        registry.add("spring.flyway.placeholders.sga_app_password", () -> "sga_app_secret_test_pass");
        registry.add("app.jwt.secret", () -> "test-container-jwt-secret-key-32-chars-long-minimum");
        registry.add("app.crypto.secret-key", () -> "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        registry.add("app.grpc.internal-token", () -> "test-grpc-token");
    }

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private AuditoriaService auditoriaService;

    @Autowired(required = false)
    private MockMvc mockMvc;

    @Test
    @Order(1)
    @DisplayName("E13.1: PostgreSQL limpio y migraciones ejecutadas en contenedor")
    void test_01_PostgresLimpioYMigraciones() {
        assertNotNull(jdbcTemplate, "JdbcTemplate debe estar inyectado y conectado a Testcontainers");

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'sga_principal' AND table_name = 'auditoria'",
                Integer.class
        );
        assertEquals(1, count, "La tabla sga_principal.auditoria debe existir tras la migracion");
    }

    @Test
    @Order(2)
    @DisplayName("E13.2 & E3: Persistencia de auditoria con formato canonico (HMAC + Lamport)")
    void test_02_PersistenciaAuditoria() {
        assertNotNull(auditoriaService, "AuditoriaService debe estar disponible");

        auditoriaService.registrarCrud("CREAR", "matricula", 8888L, "Matricula de prueba Testcontainers E13");

        Map<String, Object> fila = jdbcTemplate.queryForMap(
                "SELECT * FROM sga_principal.auditoria WHERE registro_id = 8888 LIMIT 1"
        );

        assertEquals("SECRETARIA", fila.get("schema_origen"));
        assertEquals("CREAR", String.valueOf(fila.get("accion")));
        assertEquals("matricula", fila.get("tabla_afectada"));
        assertEquals("EXITO", fila.get("resultado"));
        assertNotNull(fila.get("hmac"), "Debe persistir firma HMAC de integridad");
        assertEquals(64, ((String) fila.get("hmac")).length(), "HMAC debe tener 64 caracteres");
        assertNotNull(fila.get("reloj_lamport"), "Debe registrar reloj Lamport");
        assertTrue(((Number) fila.get("reloj_lamport")).longValue() >= 1L);
    }

    @Test
    @Order(3)
    @DisplayName("E13.3 & E6: Trigger append-only rechaza UPDATE y DELETE sobre auditoria")
    void test_03_TriggerAppendOnly_RechazaModificaciones() {
        // Asegurar que exista una fila para disparar el trigger FOR EACH ROW
        jdbcTemplate.update("""
            INSERT INTO sga_principal.auditoria (schema_origen, username, accion, tabla_afectada, registro_id, descripcion)
            VALUES ('SECRETARIA', 'admin_test', 'CREAR'::sga_principal.accion_auditoria_t, 'matricula', 8888, 'Fila base')
            ON CONFLICT DO NOTHING
        """);

        // Intento de UPDATE debe ser rechazado por el trigger
        DataAccessException exUpdate = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "UPDATE sga_principal.auditoria SET descripcion = 'modificacion fraudulenta' WHERE registro_id = 8888"
            );
        }, "El trigger append-only debe prohibir cualquier UPDATE");
        assertTrue(exUpdate.getMessage().contains("inmutable de solo adicion")
                || exUpdate.getMessage().contains("prohibir_modificacion_auditoria")
                || exUpdate.getMessage().contains("tg_auditoria_append_only"));

        // Intento de DELETE debe ser rechazado por el trigger
        DataAccessException exDelete = assertThrows(DataAccessException.class, () -> {
            jdbcTemplate.update(
                    "DELETE FROM sga_principal.auditoria WHERE registro_id = 8888"
            );
        }, "El trigger append-only debe prohibir cualquier DELETE");
        assertTrue(exDelete.getMessage().contains("inmutable de solo adicion")
                || exDelete.getMessage().contains("prohibir_modificacion_auditoria")
                || exDelete.getMessage().contains("tg_auditoria_append_only"));
    }

    @Test
    @Order(4)
    @DisplayName("E13.4 & E6: Restriccion estricta de privilegios con 2 conexiones (Admin vs App User con Trigger Desactivado)")
    void test_04_RestriccionPrivilegios_DosConexiones() throws SQLException {
        String appUser = "sga_app_test";
        String appPass = "app_secret_123";

        // CONEXION 1: Administrador configura usuario de aplicacion y revoca privilegios
        try (Connection adminConn = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
             Statement adminStmt = adminConn.createStatement()) {

            adminStmt.execute("DO $$ BEGIN " +
                    "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + appUser + "') THEN " +
                    "CREATE ROLE " + appUser + " WITH LOGIN PASSWORD '" + appPass + "' NOSUPERUSER NOCREATEDB NOCREATEROLE; " +
                    "END IF; END $$;");

            adminStmt.execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName() + " TO " + appUser + ";");
            adminStmt.execute("GRANT USAGE ON SCHEMA sga_principal TO " + appUser + ";");
            adminStmt.execute("GRANT SELECT, INSERT ON TABLE sga_principal.auditoria TO " + appUser + ";");
            adminStmt.execute("REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM " + appUser + ";");

            // Desactivar temporalmente el trigger en la base administrativa
            adminStmt.execute("ALTER TABLE sga_principal.auditoria DISABLE TRIGGER tg_auditoria_append_only;");
        }

        // CONEXION 2: Usuario de aplicacion intenta UPDATE y DELETE con el trigger desactivado
        try (Connection appConn = DriverManager.getConnection(
                postgres.getJdbcUrl(), appUser, appPass);
             Statement appStmt = appConn.createStatement()) {

            // Intento de UPDATE debe fallar a nivel de permisos de motor PostgreSQL (permission denied)
            SQLException exUpdate = assertThrows(SQLException.class, () -> {
                appStmt.executeUpdate("UPDATE sga_principal.auditoria SET descripcion = 'hack sin trigger' WHERE registro_id = 8888;");
            }, "PostgreSQL debe arrojar permission denied en UPDATE para el usuario de aplicacion");
            assertTrue(exUpdate.getMessage().toLowerCase().contains("permission denied")
                    || "42501".equals(exUpdate.getSQLState()),
                    "El error debe ser de denegacion de privilegios SQLState 42501");

            // Intento de DELETE debe fallar a nivel de permisos de motor PostgreSQL (permission denied)
            SQLException exDelete = assertThrows(SQLException.class, () -> {
                appStmt.executeUpdate("DELETE FROM sga_principal.auditoria WHERE registro_id = 8888;");
            }, "PostgreSQL debe arrojar permission denied en DELETE para el usuario de aplicacion");
            assertTrue(exDelete.getMessage().toLowerCase().contains("permission denied")
                    || "42501".equals(exDelete.getSQLState()),
                    "El error debe ser de denegacion de privilegios SQLState 42501");
        } finally {
            // Restaurar y reactivar el trigger con el usuario administrador
            try (Connection adminConn = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 Statement adminStmt = adminConn.createStatement()) {
                adminStmt.execute("ALTER TABLE sga_principal.auditoria ENABLE TRIGGER tg_auditoria_append_only;");
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("E13.5: Arranque del backend y validacion de Actuator Health")
    void test_05_ArranqueBackend_ActuatorHealth() throws Exception {
        assertNotNull(mockMvc, "MockMvc debe estar inicializado con el contexto del backend");

        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
