package ec.edu.uteq.sga.integration;

import ec.edu.uteq.sga.application.service.AuditHashService;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class AuditoriaCadenaConcurrencyE3Test {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("e3_auditoria")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("db/init/baseline_flyway_v8.sql");

    private static final String GENESIS =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditHashService hashService =
            new AuditHashService();

    @BeforeEach
    void prepararBase() throws Exception {
        /*
         * E3 / Punto 42:
         * La prueba de concurrencia debe ejecutar el pipeline real
         * de migraciones versionadas (Flyway V9-V18) en lugar de
         * construir el esquema a mano con DDL crudo.
         */
        Flyway flyway = Flyway.configure()
                .dataSource(
                        POSTGRES.getJdbcUrl(),
                        POSTGRES.getUsername(),
                        POSTGRES.getPassword()
                )
                .schemas("sga_principal")
                .defaultSchema("sga_principal")
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .baselineVersion(
                        MigrationVersion.fromVersion("8")
                )
                .load();

        flyway.migrate();

        try (
                Connection connection = nuevaConexion();
                Statement st = connection.createStatement()
        ) {
            st.execute("""
                    TRUNCATE TABLE sga_principal.auditoria RESTART IDENTITY CASCADE
                    """);

            st.execute("""
                    UPDATE sga_principal.estado_cadena_auditoria
                    SET ultimo_hash = '0000000000000000000000000000000000000000000000000000000000000000',
                        ultimo_lamport = 0,
                        vector_reloj = '{}'
                    WHERE id_estado = 1
                    """);
        }
    }

    @Test
    void dosEscritoresConcurrentesNoBifurcanLaCadena()
            throws Exception {

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);

        ExecutorService executor =
                Executors.newFixedThreadPool(2);

        try {
            Future<Evento> principal = executor.submit(
                    () -> escribirEvento(
                            "PRINCIPAL",
                            preparados,
                            iniciar
                    )
            );

            Future<Evento> secretaria = executor.submit(
                    () -> escribirEvento(
                            "SECRETARIA",
                            preparados,
                            iniciar
                    )
            );

            assertTrue(
                    preparados.await(5, TimeUnit.SECONDS),
                    "Los dos escritores deben quedar preparados"
            );

            // Ambos intentan tomar la misma fila singleton
            // aproximadamente al mismo tiempo.
            iniciar.countDown();

            Evento uno = principal.get(15, TimeUnit.SECONDS);
            Evento dos = secretaria.get(15, TimeUnit.SECONDS);

            List<Evento> resultados =
                    new ArrayList<>(List.of(uno, dos));

            resultados.sort(
                    Comparator.comparingLong(Evento::lamport)
            );

            Evento primero = resultados.get(0);
            Evento segundo = resultados.get(1);

            // Si no existiera serializacion real, ambos podrian
            // leer GENESIS/0 y producir dos hijos del mismo hash.
            assertEquals(1L, primero.lamport());
            assertEquals(2L, segundo.lamport());

            assertEquals(
                    GENESIS,
                    primero.hashAnterior()
            );

            assertEquals(
                    primero.hashActual(),
                    segundo.hashAnterior(),
                    "El segundo escritor debe observar la cabeza "
                            + "confirmada por el primero"
            );

            assertNotEquals(
                    primero.hashActual(),
                    segundo.hashActual()
            );

            verificarPersistenciaFinal(
                    primero,
                    segundo
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Evento escribirEvento(
            String escritor,
            CountDownLatch preparados,
            CountDownLatch iniciar
    ) throws Exception {

        preparados.countDown();

        assertTrue(
                iniciar.await(5, TimeUnit.SECONDS)
        );

        try (Connection connection = nuevaConexion()) {
            connection.setAutoCommit(false);

            try {
                String hashAnterior;
                long ultimoLamport;

                /*
                 * MISMO MECANISMO E3 usado por Secretaria y Docente:
                 * bloquear la fila singleton antes de leer la cabeza.
                 */
                try (
                        PreparedStatement select =
                                connection.prepareStatement("""
                                    SELECT
                                        ultimo_hash,
                                        ultimo_lamport
                                    FROM
                                        sga_principal.estado_cadena_auditoria
                                    WHERE id_estado = 1
                                    FOR UPDATE
                                    """)
                ) {
                    try (ResultSet rs = select.executeQuery()) {
                        assertTrue(
                                rs.next(),
                                "Debe existir la cabeza singleton"
                        );

                        hashAnterior =
                                rs.getString("ultimo_hash");

                        ultimoLamport =
                                rs.getLong("ultimo_lamport");
                    }
                }

                long lamport =
                        ultimoLamport + 1L;

                /*
                 * Mantener el lock un instante hace que la colision
                 * concurrente sea observable y evita una prueba que
                 * pase solo porque los hilos se ejecutaron separados.
                 */
                Thread.sleep(200);

                Map<String, Object> contenido =
                        hashService.contenidoEvento(
                                "AUDITORIA",
                                "calificacion",
                                escritor.equals("PRINCIPAL")
                                        ? 101L
                                        : 102L,
                                "EDITAR",
                                escritor.toLowerCase(),
                                escritor.equals("PRINCIPAL")
                                        ? "2026-09-13T08:30:00.100Z"
                                        : "2026-09-13T08:30:00.200Z",
                                Map.of(
                                        "schema_origen",
                                        escritor,
                                        "resultado",
                                        "EXITO"
                                ),
                                "m2",
                                lamport,
                                null,
                                "NO_APLICA"
                        );

                String canonico =
                        hashService.jsonCanonico(contenido);

                String hashActual =
                        hashService.calcularHash(
                                hashAnterior,
                                contenido
                        );

                try (
                        PreparedStatement insert =
                                connection.prepareStatement("""
                                    INSERT INTO sga_principal.auditoria
                                        (
                                            schema_origen,
                                            accion,
                                            tabla_afectada,
                                            registro_id,
                                            hash_anterior,
                                            hash_actual,
                                            reloj_lamport,
                                            contenido_canonico,
                                            version_canonica
                                        )
                                    VALUES (?, ?::sga_principal.accion_auditoria_t, 'calificacion', ?, ?, ?, ?, ?, 'v1')
                                    """)
                ) {
                    insert.setString(1, escritor);
                    insert.setString(2, "MODIFICAR");
                    insert.setLong(3, escritor.equals("PRINCIPAL") ? 101L : 102L);
                    insert.setString(4, hashAnterior);
                    insert.setString(5, hashActual);
                    insert.setLong(6, lamport);
                    insert.setString(7, canonico);

                    assertEquals(
                            1,
                            insert.executeUpdate()
                    );
                }

                try (
                        PreparedStatement update =
                                connection.prepareStatement("""
                                    UPDATE
                                        sga_principal.estado_cadena_auditoria
                                    SET
                                        ultimo_hash = ?,
                                        ultimo_lamport = ?
                                    WHERE id_estado = 1
                                    """)
                ) {
                    update.setString(
                            1,
                            hashActual
                    );

                    update.setLong(
                            2,
                            lamport
                    );

                    assertEquals(
                            1,
                            update.executeUpdate()
                    );
                }

                connection.commit();

                return new Evento(
                        escritor,
                        hashAnterior,
                        hashActual,
                        lamport
                );
            } catch (Throwable e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private void verificarPersistenciaFinal(
            Evento primero,
            Evento segundo
    ) throws Exception {

        try (Connection connection = nuevaConexion()) {
            try (
                    PreparedStatement select =
                            connection.prepareStatement("""
                                SELECT
                                    hash_anterior,
                                    hash_actual,
                                    reloj_lamport
                                FROM sga_principal.auditoria
                                ORDER BY reloj_lamport
                                """)
            ) {
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());

                    assertEquals(
                            primero.hashAnterior(),
                            rs.getString("hash_anterior")
                    );

                    assertEquals(
                            primero.hashActual(),
                            rs.getString("hash_actual")
                    );

                    assertEquals(
                            1L,
                            rs.getLong("reloj_lamport")
                    );

                    assertTrue(rs.next());

                    assertEquals(
                            primero.hashActual(),
                            rs.getString("hash_anterior")
                    );

                    assertEquals(
                            segundo.hashActual(),
                            rs.getString("hash_actual")
                    );

                    assertEquals(
                            2L,
                            rs.getLong("reloj_lamport")
                    );

                    assertFalse(
                            rs.next(),
                            "Deben existir exactamente dos eslabones"
                    );
                }
            }

            try (
                    PreparedStatement select =
                            connection.prepareStatement("""
                                SELECT
                                    ultimo_hash,
                                    ultimo_lamport
                                FROM
                                    sga_principal.estado_cadena_auditoria
                                WHERE id_estado = 1
                                """)
            ) {
                try (ResultSet rs = select.executeQuery()) {
                    assertTrue(rs.next());

                    assertEquals(
                            segundo.hashActual(),
                            rs.getString("ultimo_hash")
                    );

                    assertEquals(
                            2L,
                            rs.getLong("ultimo_lamport")
                    );
                }
            }
        }
    }

    private Connection nuevaConexion()
            throws Exception {

        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );
    }

    private record Evento(
            String escritor,
            String hashAnterior,
            String hashActual,
            long lamport
    ) {
    }
}
