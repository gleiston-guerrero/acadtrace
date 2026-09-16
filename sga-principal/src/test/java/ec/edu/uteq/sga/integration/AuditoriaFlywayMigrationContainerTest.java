package ec.edu.uteq.sga.integration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class AuditoriaFlywayMigrationContainerTest {

    private static final String DB_PASSWORD =
            UUID.randomUUID().toString();

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("e4_e13_migraciones")
                    .withUsername("e4_e13_test")
                    .withPassword(DB_PASSWORD)
                    .withInitScript(
                            "db/init/baseline_flyway_v8.sql"
                    );

    @Test
    void pipelineRealDeFlywayCreaTriggerDeInmutabilidad()
            throws SQLException {

        // El PostgreSQL efimero debe estar realmente levantado.
        assertThat(POSTGRES.isRunning()).isTrue();

        /*
         * Antes de ejecutar Flyway no debe existir ni su historial
         * ni el trigger de inmutabilidad.
         *
         * Esto demuestra que el test no fabrica manualmente
         * tg_auditoria_append_only.
         */
        try (Connection connection = abrirConexion()) {

            assertThat(
                    existeHistorialFlyway(connection)
            ).isFalse();

            assertThat(
                    obtenerDefinicionTrigger(connection)
            ).isNull();
        }

        /*
         * Ejecutar el pipeline REAL de migraciones versionadas
         * del proyecto.
         *
         * El baseline es V8, igual que en la configuracion
         * productiva. A partir de ahi Flyway ejecuta V9-V17.
         *
         * No se captura ninguna excepcion de migrate():
         * si una migracion falla, la prueba falla.
         */
        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .placeholders(java.util.Map.of("sga_app_password", "sga_app_secure_pass_2026"))
                .schemas("sga_principal")
                .defaultSchema("sga_principal")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(
                        MigrationVersion.fromVersion("8")
                )
                .load();

        flyway.migrate();

        try (Connection connection = abrirConexion()) {

            /*
             * E13:
             * demostrar que se ejecuto el pipeline real y que
             * las migraciones productivas aparecen en el historial
             * oficial de Flyway.
             */
            for (String version : List.of(
                    "9",
                    "10",
                    "12",
                    "13",
                    "14",
                    "15",
                    "16",
                    "17",
                    "18"
            )) {

                assertThat(
                        migracionAplicada(
                                connection,
                                version
                        )
                )
                        .as(
                                "La migracion V%s debe constar como aplicada",
                                version
                        )
                        .isTrue();
            }

            /*
             * E4:
             * demostrar que V13 creo realmente el trigger.
             */
            String definicionTrigger =
                    obtenerDefinicionTrigger(connection);

            assertThat(definicionTrigger)
                    .as(
                            "V13 debe crear tg_auditoria_append_only"
                    )
                    .isNotNull()
                    .containsIgnoringCase("BEFORE")
                    .containsIgnoringCase("UPDATE")
                    .containsIgnoringCase("DELETE")
                    .containsIgnoringCase("ON sga_principal.auditoria");

            /*
             * INSERT esta permitido porque la bitacora
             * debe ser append-only.
             */
            long idAuditoria =
                    insertarRegistroTestigo(connection);

            assertThat(idAuditoria)
                    .isPositive();

            /*
             * UPDATE debe ser rechazado por el trigger creado
             * realmente por la migracion V13.
             */
            comprobarOperacionRechazada(
                    """
                    UPDATE sga_principal.auditoria
                    SET descripcion = 'modificacion prohibida'
                    WHERE id_auditoria = %d
                    """.formatted(idAuditoria)
            );

            /*
             * Verificar que el dato original no fue modificado.
             */
            assertThat(
                    obtenerDescripcion(
                            connection,
                            idAuditoria
                    )
            ).isEqualTo(
                    "registro original e4 e13"
            );

            /*
             * DELETE tambien debe ser rechazado.
             */
            comprobarOperacionRechazada(
                    """
                    DELETE FROM sga_principal.auditoria
                    WHERE id_auditoria = %d
                    """.formatted(idAuditoria)
            );

            /*
             * El registro debe continuar existiendo despues
             * de los intentos de UPDATE y DELETE.
             */
            assertThat(
                    contarRegistro(
                            connection,
                            idAuditoria
                    )
            ).isEqualTo(1L);
        }
    }

    @Test
    void criterioE6_rolCreadoPorMigracionRechazaModificacionInclusoSinTrigger()
            throws SQLException {

        assertThat(POSTGRES.isRunning()).isTrue();

        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .placeholders(java.util.Map.of("sga_app_password", "sga_app_secure_pass_2026"))
                .schemas("sga_principal")
                .defaultSchema("sga_principal")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(MigrationVersion.fromVersion("8"))
                .load();

        flyway.migrate();

        // 1. Demostrar que la migracion V18 creo realmente el rol sga_app en el motor
        try (Connection adminConn = abrirConexion();
             PreparedStatement ps = adminConn.prepareStatement(
                     "SELECT count(*) FROM pg_roles WHERE rolname = 'sga_app'")) {
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1))
                        .as("El rol sga_app debe haber sido creado por la migracion V18")
                        .isEqualTo(1);
            }
        }

        // 2. Insertar registro testigo con conexion admin
        long idAuditoria;
        try (Connection adminConn = abrirConexion()) {
            idAuditoria = insertarRegistroTestigo(adminConn);

            // 3. Simular que un atacante o admin desactiva temporalmente el trigger
            try (Statement stmt = adminConn.createStatement()) {
                stmt.execute("ALTER TABLE sga_principal.auditoria DISABLE TRIGGER tg_auditoria_append_only;");
            }
        }

        // 4. Conectarse con el usuario de aplicacion sga_app creado por la migracion
        try {
            try (Connection appConn = DriverManager.getConnection(
                    POSTGRES.getJdbcUrl(), "sga_app", "sga_app_secure_pass_2026")) {

                // Intento de UPDATE sin trigger: DEBE fallar por permisos a nivel de motor (42501 permission denied)
                try (Statement stmt = appConn.createStatement()) {
                    assertThatThrownBy(() -> stmt.executeUpdate(
                            "UPDATE sga_principal.auditoria SET descripcion = 'hack sin trigger' WHERE id_auditoria = " + idAuditoria))
                            .isInstanceOf(SQLException.class)
                            .satisfies(ex -> {
                                SQLException sqlEx = (SQLException) ex;
                                assertThat(sqlEx.getSQLState()).isEqualTo("42501");
                                assertThat(sqlEx.getMessage().toLowerCase()).contains("permission denied");
                            });

                    // Intento de DELETE sin trigger: DEBE fallar por permisos a nivel de motor (42501 permission denied)
                    assertThatThrownBy(() -> stmt.executeUpdate(
                            "DELETE FROM sga_principal.auditoria WHERE id_auditoria = " + idAuditoria))
                            .isInstanceOf(SQLException.class)
                            .satisfies(ex -> {
                                SQLException sqlEx = (SQLException) ex;
                                assertThat(sqlEx.getSQLState()).isEqualTo("42501");
                                assertThat(sqlEx.getMessage().toLowerCase()).contains("permission denied");
                            });
                }
            }
        } finally {
            // Reactivar trigger con conexion admin
            try (Connection adminConn = abrirConexion();
                 Statement stmt = adminConn.createStatement()) {
                stmt.execute("ALTER TABLE sga_principal.auditoria ENABLE TRIGGER tg_auditoria_append_only;");
            }
        }
    }

    private static Connection abrirConexion()
            throws SQLException {

        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }

    private static boolean existeHistorialFlyway(
            Connection connection
    ) throws SQLException {

        try (
                Statement statement =
                        connection.createStatement();

                ResultSet resultSet =
                        statement.executeQuery(
                                """
                                SELECT to_regclass(
                                    'sga_principal.flyway_schema_history'
                                ) IS NOT NULL
                                """
                        )
        ) {

            resultSet.next();

            return resultSet.getBoolean(1);
        }
    }

    private static boolean migracionAplicada(
            Connection connection,
            String version
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*)
                                FROM sga_principal.flyway_schema_history
                                WHERE version = ?
                                  AND success = TRUE
                                """
                        )
        ) {

            statement.setString(
                    1,
                    version
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                resultSet.next();

                return resultSet.getLong(1) == 1L;
            }
        }
    }

    private static String obtenerDefinicionTrigger(
            Connection connection
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT pg_get_triggerdef(t.oid)
                                FROM pg_trigger t
                                JOIN pg_class c
                                  ON c.oid = t.tgrelid
                                JOIN pg_namespace n
                                  ON n.oid = c.relnamespace
                                WHERE n.nspname = 'sga_principal'
                                  AND c.relname = 'auditoria'
                                  AND t.tgname =
                                      'tg_auditoria_append_only'
                                  AND NOT t.tgisinternal
                                """
                        );

                ResultSet resultSet =
                        statement.executeQuery()
        ) {

            if (!resultSet.next()) {
                return null;
            }

            return resultSet.getString(1);
        }
    }

    private static long insertarRegistroTestigo(
            Connection connection
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                INSERT INTO sga_principal.auditoria (
                                    schema_origen,
                                    accion,
                                    tabla_afectada,
                                    registro_id,
                                    descripcion,
                                    resultado,
                                    trace_id
                                )
                                VALUES (
                                    'PRINCIPAL',
                                    'CREAR',
                                    'e4_e13_migraciones',
                                    41313,
                                    'registro original e4 e13',
                                    'EXITO',
                                    gen_random_uuid()
                                )
                                RETURNING id_auditoria
                                """
                        )
        ) {

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                resultSet.next();

                return resultSet.getLong(1);
            }
        }
    }

    private static void comprobarOperacionRechazada(
            String sql
    ) {

        assertThatThrownBy(
                () -> {
                    try (
                            Connection connection =
                                    abrirConexion();

                            Statement statement =
                                    connection.createStatement()
                    ) {

                        statement.executeUpdate(sql);
                    }
                }
        )
                .isInstanceOf(SQLException.class)
                .hasMessageContaining(
                        "Operacion rechazada"
                );
    }

    private static String obtenerDescripcion(
            Connection connection,
            long idAuditoria
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT descripcion
                                FROM sga_principal.auditoria
                                WHERE id_auditoria = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    idAuditoria
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                assertThat(
                        resultSet.next()
                ).isTrue();

                return resultSet.getString(1);
            }
        }
    }

    private static long contarRegistro(
            Connection connection,
            long idAuditoria
    ) throws SQLException {

        try (
                PreparedStatement statement =
                        connection.prepareStatement(
                                """
                                SELECT COUNT(*)
                                FROM sga_principal.auditoria
                                WHERE id_auditoria = ?
                                """
                        )
        ) {

            statement.setLong(
                    1,
                    idAuditoria
            );

            try (
                    ResultSet resultSet =
                            statement.executeQuery()
            ) {

                resultSet.next();

                return resultSet.getLong(1);
            }
        }
    }
}
