package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuditoriaInmutabilidadTest {

    @Autowired(required = false)
    private AuditoriaRepository auditoriaRepository;

    @Autowired(required = false)
    private DataSource dataSource;

    @Test
    @DisplayName("Criterio E4 y E6: Verificar que la bitacora rechaza eliminaciones y modificaciones")
    @Transactional
    void verificarInmutabilidad_bloqueaDeleteYUpdate() {
        assertNotNull(auditoriaRepository, "El repositorio de auditoria debe estar inyectado y disponible; la prueba no puede omitirse");

        // 1. Inserción permitida (Append-only)
        Auditoria nuevo = Auditoria.builder()
                .schemaOrigen("PRINCIPAL")
                .accion("CREAR")
                .tablaAfectada("test_inmutabilidad")
                .registroId(99999L)
                .descripcion("Registro inicial de prueba append-only")
                .traceId(UUID.randomUUID())
                .resultado("EXITO")
                .build();

        Auditoria guardado = auditoriaRepository.saveAndFlush(nuevo);
        assertNotNull(guardado.getIdAuditoria(), "El registro debe persistirse exitosamente en insercion append-only");

        // 2. Comprobar que UPDATE es rechazado por el trigger
        guardado.setDescripcion("Intento de modificacion fraudulenta");
        assertThrows(Exception.class, () -> {
            auditoriaRepository.saveAndFlush(guardado);
        }, "Criterio E4: La base de datos debe rechazar cualquier UPDATE");

        // 3. Comprobar que DELETE es rechazado por el trigger y revocacion
        assertThrows(Exception.class, () -> {
            auditoriaRepository.delete(guardado);
            auditoriaRepository.flush();
        }, "Criterio E4 y E6: La base de datos debe rechazar cualquier DELETE");
    }

    @Test
    @DisplayName("Criterio E6: Demostrar que el usuario de aplicacion no puede actualizar ni borrar aunque el trigger sea desactivado temporalmente")
    void verificarRestriccionPrivilegios_dosConexiones_fallaInclusoSinTrigger() throws SQLException {
        assertNotNull(dataSource, "El DataSource debe estar inyectado y disponible; la prueba no puede omitirse");

        String appUser = "sga_app_test_e6";
        String appPass = "sga_app_test_pass_123";

        try (Connection adminConn = dataSource.getConnection()) {
            String jdbcUrl = adminConn.getMetaData().getURL();

            try (Statement adminStmt = adminConn.createStatement()) {
                // 1. Conexión Administrativa: Configurar rol de aplicacion y revocar privilegios UPDATE/DELETE
                adminStmt.execute("DO $$ BEGIN " +
                        "IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = '" + appUser + "') THEN " +
                        "CREATE ROLE " + appUser + " WITH LOGIN PASSWORD '" + appPass + "' NOSUPERUSER NOCREATEDB NOCREATEROLE; " +
                        "END IF; END $$;");

                adminStmt.execute("GRANT USAGE ON SCHEMA sga_principal TO " + appUser + ";");
                adminStmt.execute("GRANT SELECT, INSERT ON TABLE sga_principal.auditoria TO " + appUser + ";");
                adminStmt.execute("REVOKE UPDATE, DELETE, TRUNCATE ON TABLE sga_principal.auditoria FROM " + appUser + ";");

                // 2. Insertar registro testigo
                adminStmt.execute("INSERT INTO sga_principal.auditoria " +
                        "(schema_origen, accion, tabla_afectada, registro_id, descripcion, resultado, trace_id) " +
                        "VALUES ('PRINCIPAL', 'CREAR', 'test_e6_privilegios', 77777, 'Registro testigo E6', 'EXITO', gen_random_uuid());");

                // 3. Desactivar temporalmente el trigger en la base administrativa
                adminStmt.execute("ALTER TABLE sga_principal.auditoria DISABLE TRIGGER tg_auditoria_append_only;");
            }

            try {
                // 4. Conexión de Usuario de Aplicación: Intentar UPDATE y DELETE sin trigger
                try (Connection appConn = DriverManager.getConnection(jdbcUrl, appUser, appPass);
                     Statement appStmt = appConn.createStatement()) {

                    // Intento de UPDATE: debe fallar por denegación de privilegios a nivel de motor (42501)
                    SQLException exUpdate = assertThrows(SQLException.class, () -> {
                        appStmt.executeUpdate("UPDATE sga_principal.auditoria SET descripcion = 'modificacion sin trigger' WHERE registro_id = 77777;");
                    }, "PostgreSQL debe arrojar permission denied en UPDATE para el usuario de aplicacion aunque no haya trigger");
                    assertTrue(exUpdate.getMessage().toLowerCase().contains("permission denied")
                            || "42501".equals(exUpdate.getSQLState()),
                            "El error debe ser de denegacion de privilegios SQLState 42501");

                    // Intento de DELETE: debe fallar por denegación de privilegios a nivel de motor (42501)
                    SQLException exDelete = assertThrows(SQLException.class, () -> {
                        appStmt.executeUpdate("DELETE FROM sga_principal.auditoria WHERE registro_id = 77777;");
                    }, "PostgreSQL debe arrojar permission denied en DELETE para el usuario de aplicacion aunque no haya trigger");
                    assertTrue(exDelete.getMessage().toLowerCase().contains("permission denied")
                            || "42501".equals(exDelete.getSQLState()),
                            "El error debe ser de denegacion de privilegios SQLState 42501");
                }
            } finally {
                // 5. Limpieza y Reactivación con la Conexión Administrativa
                try (Statement cleanupStmt = adminConn.createStatement()) {
                    cleanupStmt.execute("ALTER TABLE sga_principal.auditoria ENABLE TRIGGER tg_auditoria_append_only;");
                    cleanupStmt.execute("DELETE FROM sga_principal.auditoria WHERE registro_id = 77777;");
                }
            }
        }
    }
}