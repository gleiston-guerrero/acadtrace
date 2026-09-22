package ec.edu.uteq.sga.integration;

import ec.edu.uteq.sga.application.service.AuditHashService;
import ec.edu.uteq.sga.application.service.AuditoriaService;
import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.domain.entity.EstadoCadenaAuditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.repository.EstadoCadenaAuditoriaRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Complementa AuditoriaCadenaConcurrencyE3Test.
 *
 * AuditoriaCadenaConcurrencyE3Test comprueba el mecanismo de PostgreSQL
 * ejecutando el SELECT FOR UPDATE y el INSERT directamente desde el
 * codigo de la prueba. Ese test cubre el escenario de concurrencia a
 * nivel de motor.
 *
 * Esta prueba cubre la otra mitad: ejecuta la misma escena de
 * concurrencia pero delegando en AuditoriaService, el bean real de
 * produccion. Asi se valida que el codigo que se despliega mantiene
 * la cadena integra cuando dos hilos escriben simultaneamente y no
 * solo el motor Postgres.
 */
@SpringBootTest
@Testcontainers
class AuditoriaCadenaConcurrencyServicioE3Test {

    private static final String SGA_APP_PASSWORD =
            System.getenv().getOrDefault("SGA_APP_PASSWORD_TEST",
                    "test_pass_123");

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("e37_servicio")
                    .withUsername("postgres")
                    .withPassword("postgres")
                    .withInitScript("db/migration/V8__baseline_completo.sql");


    @BeforeAll
    static void initSgaApp() throws Exception {
        try (Connection conn = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {
            stmt.execute("DO $$ BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='sga_app') THEN CREATE ROLE sga_app WITH LOGIN PASSWORD '" + SGA_APP_PASSWORD + "' NOSUPERUSER NOCREATEDB NOCREATEROLE; END IF; END $$;");
            stmt.execute("GRANT CONNECT ON DATABASE " + POSTGRES.getDatabaseName() + " TO sga_app;");
            stmt.execute("GRANT USAGE ON SCHEMA public TO sga_app;");
        }
    }

    @DynamicPropertySource
    static void configurarPropiedades(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", () -> "sga_app");
        registry.add("spring.datasource.password", () -> SGA_APP_PASSWORD);
        registry.add("spring.flyway.user", POSTGRES::getUsername);
        registry.add("spring.flyway.password", POSTGRES::getPassword);
        registry.add("spring.flyway.placeholders.sga_app_password", () -> SGA_APP_PASSWORD);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "8");
        registry.add("AUDIT", () -> "m2");
    }

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private EstadoCadenaAuditoriaRepository estadoCadenaRepository;

    @Test
    void dosEscriturasConcurrentesViaServicioProducen2Eslabones() throws Exception {
        assertThat(auditoriaService)
                .as("El bean AuditoriaService debe estar disponible en el contexto")
                .isNotNull();

        auditoriaService.setAuditMode("m2");

        // Limpiar la bitacora antes del test para asegurar estado determinista
        try (Connection conn = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement st = conn.createStatement()) {
            st.execute("ALTER TABLE sga_principal.auditoria DISABLE TRIGGER ALL");
            st.execute("DELETE FROM sga_principal.auditoria");
            st.execute("ALTER SEQUENCE sga_principal.auditoria_id_auditoria_seq RESTART WITH 1");
            st.execute("ALTER TABLE sga_principal.auditoria ENABLE TRIGGER ALL");
            st.execute("""
                    UPDATE sga_principal.estado_cadena_auditoria
                    SET ultimo_hash = '0000000000000000000000000000000000000000000000000000000000000000',
                        ultimo_lamport = 0,
                        vector_reloj = '{}'
                    WHERE id_estado = 1
                    """);
        }

        int hilos = 2;
        CountDownLatch listos = new CountDownLatch(hilos);
        CountDownLatch salida = new CountDownLatch(1);
        CountDownLatch fin = new CountDownLatch(hilos);
        ExecutorService pool = Executors.newFixedThreadPool(hilos);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < hilos; i++) {
            final int indice = i;
            pool.submit(() -> {
                try {
                    listos.countDown();
                    salida.await();
                    auditoriaService.registrarCrud(
                            "CREAR",
                            "estudiante",
                            (long) (100 + indice),
                            "Escritura concurrente servicio " + indice
                    );
                } catch (Throwable t) {
                    errores.add(t);
                } finally {
                    fin.countDown();
                }
            });
        }

        assertThat(listos.await(5, TimeUnit.SECONDS)).isTrue();
        salida.countDown();
        assertThat(fin.await(30, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(errores).as("Ningun hilo concurrente debio fallar").isEmpty();

        // Comprobar que se generaron 2 eslabones consecutivos y con cadena de hash valida
        List<Auditoria> eventos = auditoriaRepository.findAll(Sort.by("relojLamport").ascending());
        assertThat(eventos).as("Deben existir exactamente 2 eventos persistidos").hasSize(2);

        Auditoria e1 = eventos.get(0);
        Auditoria e2 = eventos.get(1);

        assertThat(e1.getRelojLamport()).isEqualTo(1L);
        assertThat(e2.getRelojLamport()).isEqualTo(2L);

        assertThat(e1.getHashAnterior()).isEqualTo(AuditHashService.GENESIS_HASH);
        assertThat(e2.getHashAnterior()).isEqualTo(e1.getHashActual());

        assertThat(e1.getVersionCanonica()).isEqualTo("v1");
        assertThat(e2.getVersionCanonica()).isEqualTo("v1");

        // Comprobar que el singleton refleja el estado del segundo eslabon
        EstadoCadenaAuditoria estado = estadoCadenaRepository.findById((short) 1).orElseThrow();
        assertThat(estado.getUltimoHash()).isEqualTo(e2.getHashActual());
        assertThat(estado.getUltimoLamport()).isEqualTo(2L);
    }
}
