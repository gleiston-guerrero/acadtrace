package ec.uteq.sga.soporte.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de observabilidad para los endpoints de Spring Boot Actuator.
 * 
 * NOTA METODOLÓGICA: Esta prueba utiliza un contexto aislado de Spring (TestConfig)
 * para comprobar exclusivamente la correcta exposición, contrato HTTP (200 OK) y formato
 * de /actuator/health y /actuator/prometheus, sin requerir instancias externas de PostgreSQL,
 * servidor gRPC ni clúster etcd. No representa una prueba de integración integral de todo
 * el microservicio con sus componentes de persistencia e infraestructura.
 */
@SpringBootTest(classes = ActuatorEndpointsTest.TestConfig.class)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "management.endpoints.web.exposure.include=health,info,prometheus,metrics",
        "management.endpoint.health.probes.enabled=true",
        "management.prometheus.metrics.export.enabled=true"
})
@DisplayName("Pruebas de Observabilidad: Endpoints de Spring Boot Actuator (/actuator/health y /actuator/prometheus)")
class ActuatorEndpointsTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerFactoryAutoConfiguration.class,
            net.devh.boot.grpc.server.autoconfigure.GrpcServerMetricAutoConfiguration.class
    })
    static class TestConfig {
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("1. /actuator/health -- Retorna HTTP 200 y status UP")
    void healthEndpoint_returnsHttp200AndUp() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("2. /actuator/prometheus -- Retorna HTTP 200 y métrica JVM estable (jvm_memory_used_bytes)")
    void prometheusEndpoint_returnsHttp200AndJvmMetrics() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("jvm_memory_used_bytes")));
    }
}
