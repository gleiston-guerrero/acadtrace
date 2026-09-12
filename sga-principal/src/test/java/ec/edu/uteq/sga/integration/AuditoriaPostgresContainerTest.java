package ec.edu.uteq.sga.integration;

import ec.edu.uteq.sga.domain.entity.Auditoria;
import ec.edu.uteq.sga.infrastructure.repository.AuditoriaRepository;
import ec.edu.uteq.sga.application.service.AuditoriaService;
import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class AuditoriaPostgresContainerTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("db/init/V0__baseline.sql");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Hibernate crea las tablas; Flyway se desactiva porque V9+ asume datos de produccion
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create");
        registry.add("spring.flyway.enabled", () -> "false");
        // Usar puerto aleatorio para gRPC y Tomcat para evitar conflictos
        registry.add("grpc.server.port", () -> 0);
    }

    @Autowired
    private AuditoriaService auditoriaService;

    @Autowired
    private AuditoriaRepository auditoriaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;

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
                "INSERT",
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
}
