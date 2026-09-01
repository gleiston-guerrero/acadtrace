package ec.uteq.sga.soporte.common;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ec.uteq.sga.soporte.common.jdbc.GenericRowMapper;
import ec.uteq.sga.soporte.infrastructure.logging.MemoryLogAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("Pruebas Unitarias: Clases Comunes, Exception Handler, Mapper y Logger en Memoria")
class CommonAndLoggingTest {

    @Test
    @DisplayName("1. ApiException -- Códigos de estado y mensajes")
    void apiException_statusCodes() {
        ApiException notFound = ApiException.notFound("No encontrado");
        assertThat(notFound.getStatus()).isEqualTo(404);
        assertThat(notFound.getMessage()).isEqualTo("No encontrado");

        ApiException forbidden = ApiException.forbidden("Prohibido");
        assertThat(forbidden.getStatus()).isEqualTo(403);

        ApiException conflict = ApiException.conflict("Conflicto");
        assertThat(conflict.getStatus()).isEqualTo(409);

        ApiException badRequest = ApiException.badRequest("Invalido");
        assertThat(badRequest.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("2. PageResult -- Estructura de paginación y cálculo de páginas")
    void pageResult_paginationCalculation() {
        PageResult<String> page = PageResult.of(List.of("A", "B"), 20, 1, 10);

        assertThat(page.data()).hasSize(2);
        assertThat(page.meta().total()).isEqualTo(20);
        assertThat(page.meta().page()).isEqualTo(1);
        assertThat(page.meta().limit()).isEqualTo(10);
        assertThat(page.meta().pages()).isEqualTo(2);
    }

    @Test
    @DisplayName("3. GlobalExceptionHandler -- Manejo de ApiException y Errores Genéricos")
    void globalExceptionHandler_handlesExceptions() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        HttpServletRequest mockReq = mock(HttpServletRequest.class);
        given(mockReq.getMethod()).willReturn("GET");
        given(mockReq.getRequestURI()).willReturn("/api/soporte/test");

        ResponseEntity<Map<String, Object>> respApi = handler.handleApiException(ApiException.notFound("Ticket 99"));
        assertThat(respApi.getStatusCode().value()).isEqualTo(404);
        assertThat(respApi.getBody()).containsEntry("error", "Ticket 99");

        ResponseEntity<Map<String, Object>> respGen = handler.handleGeneric(new RuntimeException("Crash"), mockReq);
        assertThat(respGen.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respGen.getBody()).containsEntry("error", "Error interno del servidor");
    }

    @Test
    @DisplayName("4. GenericRowMapper -- Mapeo de ResultSet")
    void genericRowMapper_mapsColumns() throws SQLException {
        GenericRowMapper mapper = GenericRowMapper.INSTANCE;
        ResultSet rs = mock(ResultSet.class);
        ResultSetMetaData meta = mock(ResultSetMetaData.class);

        given(rs.getMetaData()).willReturn(meta);
        given(meta.getColumnCount()).willReturn(2);
        given(meta.getColumnLabel(1)).willReturn("id");
        given(meta.getColumnType(1)).willReturn(Types.BIGINT);
        given(rs.getObject(1)).willReturn(100L);

        given(meta.getColumnLabel(2)).willReturn("titulo");
        given(meta.getColumnType(2)).willReturn(Types.VARCHAR);
        given(rs.getObject(2)).willReturn("Falla");

        Map<String, Object> result = mapper.mapRow(rs, 1);
        assertThat(result).containsEntry("id", 100L);
        assertThat(result).containsEntry("titulo", "Falla");
    }

    @Test
    @DisplayName("5. MemoryLogAppender -- Captura de eventos WARN/ERROR")
    void memoryLogAppender_capturesLogs() {
        TestableMemoryLogAppender appender = new TestableMemoryLogAppender();
        appender.start();

        ILoggingEvent warnEvent = mock(ILoggingEvent.class);
        given(warnEvent.getLevel()).willReturn(Level.WARN);
        given(warnEvent.getTimeStamp()).willReturn(System.currentTimeMillis());
        given(warnEvent.getLoggerName()).willReturn("ec.uteq.sga.soporte.TestLogger");
        given(warnEvent.getFormattedMessage()).willReturn("Advertencia de prueba");
        IThrowableProxy throwableProxy = mock(IThrowableProxy.class);
        given(throwableProxy.getClassName()).willReturn("NullPointerException");
        given(throwableProxy.getMessage()).willReturn("null ref");
        given(warnEvent.getThrowableProxy()).willReturn(throwableProxy);

        appender.testAppend(warnEvent);

        List<Map<String, Object>> ultimos = MemoryLogAppender.ultimos(5);
        assertThat(ultimos).isNotEmpty();

        ILoggingEvent infoEvent = mock(ILoggingEvent.class);
        given(infoEvent.getLevel()).willReturn(Level.INFO);
        appender.testAppend(infoEvent);
    }

    private static class TestableMemoryLogAppender extends MemoryLogAppender {
        public void testAppend(ILoggingEvent event) {
            super.append(event);
        }
    }
}
