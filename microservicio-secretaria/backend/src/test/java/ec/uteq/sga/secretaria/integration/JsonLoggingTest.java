package ec.uteq.sga.secretaria.integration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.uteq.sga.secretaria.infrastructure.logging.JsonStructuredLayout;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias: Logging Estructurado JSON en Secretaría")
class JsonLoggingTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Generar log estructurado en JSON con service, traceId, level y message")
    void test_JsonStructuredLayout_EmitsValidJson() throws Exception {
        JsonStructuredLayout layout = new JsonStructuredLayout();
        layout.setServiceName("microservicio-secretaria");
        layout.start();

        Logger logger = (Logger) LoggerFactory.getLogger("ec.uteq.sga.secretaria.test");

        MDC.put("traceId", "trace-test-uuid-999");
        MDC.put("user", "secretaria1");

        LoggingEvent event = new LoggingEvent(
                "ec.uteq.sga.secretaria.test.TestClass",
                logger,
                Level.INFO,
                "Matrícula registrada exitosamente para el estudiante con ID 105",
                null,
                null
        );

        String jsonOutput = layout.doLayout(event);
        MDC.clear();

        assertThat(jsonOutput).isNotBlank();

        // Validar que sea un JSON sintácticamente válido
        JsonNode root = objectMapper.readTree(jsonOutput);

        assertThat(root.has("timestamp")).isTrue();
        assertThat(root.get("service").asText()).isEqualTo("microservicio-secretaria");
        assertThat(root.get("level").asText()).isEqualTo("INFO");
        assertThat(root.get("traceId").asText()).isEqualTo("trace-test-uuid-999");
        assertThat(root.get("message").asText()).contains("Matrícula registrada exitosamente");
        assertThat(root.get("logger").asText()).isEqualTo("ec.uteq.sga.secretaria.test");
    }

    @Test
    @DisplayName("Generar log estructurado con información de excepción (stack trace)")
    void test_JsonStructuredLayout_WithException() throws Exception {
        JsonStructuredLayout layout = new JsonStructuredLayout();
        layout.start();

        Logger logger = (Logger) LoggerFactory.getLogger("ec.uteq.sga.secretaria.error");

        Exception exception = new IllegalArgumentException("Cédula ecuatoriana inválida: 123");
        LoggingEvent event = new LoggingEvent(
                "ec.uteq.sga.secretaria.error.ErrorClass",
                logger,
                Level.ERROR,
                "Fallo al validar los datos del estudiante",
                exception,
                null
        );

        String jsonOutput = layout.doLayout(event);
        JsonNode root = objectMapper.readTree(jsonOutput);

        assertThat(root.get("level").asText()).isEqualTo("ERROR");
        assertThat(root.has("exception")).isTrue();
        assertThat(root.get("exception").get("className").asText()).isEqualTo("java.lang.IllegalArgumentException");
        assertThat(root.get("exception").get("message").asText()).contains("Cédula ecuatoriana inválida");
    }
}
