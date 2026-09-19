package ec.edu.uteq.sga.integration;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.domain.entity.EstadoCadenaAuditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.infrastructure.repository.EstadoCadenaAuditoriaRepository;
import ec.edu.uteq.sga.application.service.AuditoriaService;
import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditoriaPostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/migration/V8__baseline_completo.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // Secreto efimero generado solo durante la ejecucion del test.
        // No se versiona ninguna clave real ni de prueba.
        final String jwtPrueba =
                UUID.randomUUID().toString() + UUID.randomUUID().toString();
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

        // Secreto exclusivo del contexto de prueba.
        // Evita depender de JWT_SECRET del entorno local o de produccion.
        registry.add(
                "JWT_SECRET",
                () -> jwtPrueba
        );

        // Propiedades JWT exclusivas del contexto de prueba.
        registry.add(
                "jwt.secret",
                () -> jwtPrueba
        );
        registry.add(
                "jwt.expiration",
                () -> "86400000"
        );

        // Flyway crea las tablas via migraciones; Hibernate en none como en produccion
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
        registry.add("spring.flyway.baseline-on-migrate", () -> "true");
        registry.add("spring.flyway.baseline-version", () -> "8");
        registry.add("spring.flyway.schemas", () -> "sga_principal");
        registry.add("spring.flyway.default-schema", () -> "sga_principal");
        registry.add("spring.flyway.placeholders.sga_app_password", () -> "sga_app_secret_test_pass");
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
        registry.add("GRPC_INTERNAL_TOKEN", () -> "test-grpc-token");
        registry.add("app.grpc.internal-token", () -> "test-grpc-token");
        registry.add("app.notifications.internal-token", () -> "test-grpc-token");
        registry.add("MAIL_PASSWORD", () -> "test-mail-password");
        registry.add("spring.mail.password", () -> "test-mail-password");
        // Usar puerto aleatorio para gRPC y Tomcat para evitar conflictos
        registry.add("grpc.server.port", () -> 0);
    }

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private AuditoriaRepository auditoriaRepository;

    @Autowired
    private EstadoCadenaAuditoriaRepository estadoCadenaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararCabezaCadena() {
        auditoriaService.setAuditMode("m2");

        if (estadoCadenaRepository.findById((short) 1).isEmpty()) {
            estadoCadenaRepository.saveAndFlush(
                    EstadoCadenaAuditoria.builder()
                            .idEstado((short) 1)
                            .ultimoHash(
                                    "0000000000000000000000000000000000000000000000000000000000000000"
                            )
                            .ultimoLamport(0L)
                            .vectorReloj("{}")
                            .build()
            );
        }
    }

    @Test
    void usaPostgresTemporalYGuardaAuditoriaConCamposCriptograficos() {
        // 1. Verificar que el contenedor levantó y corre PostgreSQL temporal
        assertThat(postgres.isRunning()).isTrue();

        // 2. Crear usuario temporal de prueba
        Usuario usuario = Usuario.builder()
                .username("juan.perez")
                .correo("juan@test.com")
                .passwordHash("secretHash")
                .estado(true)
                .build();
        usuario = usuarioRepository.save(usuario);

        // 3. Registrar un evento de auditoria real
        auditoriaService.registrarCrud(
                "CREAR",
                "tabla_prueba",
                1L,
                "Insercion de prueba en contenedor"
        );

        // 4. Recuperar la auditoría
        List<Auditoria> eventos = auditoriaRepository.findAll();
        assertThat(eventos).isNotEmpty();
        
        Auditoria guardada = eventos.get(0);
        
        // 5. Verificar campos criptográficos generados por la migración y el servicio
        assertThat(guardada.getHashAnterior()).isNotNull();
        assertThat(guardada.getHashActual()).isNotNull();
        assertThat(guardada.getRelojLamport()).isNotNull().isGreaterThanOrEqualTo(1L);
        assertThat(guardada.getHmac()).isNotNull();
    }

    @Test
    void dosEscriturasConcurrentesMantienenUnaSolaCadena() throws Exception {
        EstadoCadenaAuditoria estadoInicial = estadoCadenaRepository
                .findById((short) 1)
                .orElseThrow();

        String hashInicial = estadoInicial.getUltimoHash();
        long lamportInicial = estadoInicial.getUltimoLamport();

        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch salida = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> primero = executor.submit(() -> {
                preparados.countDown();
                salida.await();

                auditoriaService.registrarCrud(
                        "CREAR",
                        "concurrencia_e3",
                        9001L,
                        "Evento concurrente A"
                );
                return null;
            });

            Future<?> segundo = executor.submit(() -> {
                preparados.countDown();
                salida.await();

                auditoriaService.registrarCrud(
                        "CREAR",
                        "concurrencia_e3",
                        9002L,
                        "Evento concurrente B"
                );
                return null;
            });

            assertThat(preparados.await(10, TimeUnit.SECONDS)).isTrue();

            // Ambos escritores intentan entrar al mismo tiempo.
            salida.countDown();

            primero.get(30, TimeUnit.SECONDS);
            segundo.get(30, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        List<Auditoria> eventos = auditoriaRepository
                .findByTablaAfectada("concurrencia_e3");

        assertThat(eventos).hasSize(2);

        eventos.sort(
                Comparator.comparing(Auditoria::getRelojLamport)
        );

        Auditoria evento1 = eventos.get(0);
        Auditoria evento2 = eventos.get(1);

        // Primer escritor parte de la cabeza previa.
        assertThat(evento1.getHashAnterior())
                .isEqualTo(hashInicial);

        // Segundo escritor debe enlazarse al primero:
        // no puede reutilizar la misma cabeza.
        assertThat(evento2.getHashAnterior())
                .isEqualTo(evento1.getHashActual());

        assertThat(evento1.getHashActual())
                .isNotEqualTo(evento2.getHashActual());

        // Lamport tambien queda serializado por la misma fila bloqueada.
        assertThat(evento1.getRelojLamport())
                .isEqualTo(lamportInicial + 1L);

        assertThat(evento2.getRelojLamport())
                .isEqualTo(lamportInicial + 2L);

        EstadoCadenaAuditoria estadoFinal = estadoCadenaRepository
                .findById((short) 1)
                .orElseThrow();

        assertThat(estadoFinal.getUltimoHash())
                .isEqualTo(evento2.getHashActual());

        assertThat(estadoFinal.getUltimoLamport())
                .isEqualTo(lamportInicial + 2L);
    }

}
