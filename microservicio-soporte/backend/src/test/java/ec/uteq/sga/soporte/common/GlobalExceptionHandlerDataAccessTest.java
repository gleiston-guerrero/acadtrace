package ec.uteq.sga.soporte.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerDataAccessTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void erroresSqlConocidos_seConviertenEnRespuestasDeNegocio() {
        assertSqlState("23505", 409, "Registro duplicado");
        assertSqlState("23503", 400, "Referencia inválida");
        assertSqlState("22P02", 400, "Tipo de dato inválido");
    }

    @Test
    void errorSqlDesconocido_retorna500ConDetalleTecnico() {
        ResponseEntity<Map<String, Object>> response = handler.handleDataAccess(
                new DataAccessResourceFailureException("fallo", new SQLException("sin conexion", "08006")), request());

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody()).containsEntry("error", "Error interno del servidor")
                .containsEntry("detalle", "fallo");
    }

    @Test
    void erroresDeEntrada_retorna422ConElCampoEsperado() {
        ResponseEntity<Map<String, Object>> type = handler.handleTypeMismatch(
                new MethodArgumentTypeMismatchException("abc", Long.class, "id", null, null));
        ResponseEntity<Map<String, Object>> unreadable = handler.handleUnreadable(
                new HttpMessageNotReadableException("json invalido"));
        ResponseEntity<Map<String, Object>> date = handler.handleDateParse(
                new DateTimeParseException("fecha invalida", "no-fecha", 0));

        assertThat(type.getStatusCode().value()).isEqualTo(422);
        assertThat(type.getBody().get("detalles").toString()).contains("id");
        assertThat(unreadable.getBody().get("detalles").toString()).contains("body");
        assertThat(date.getBody().get("detalles").toString()).contains("fecha");
    }

    private void assertSqlState(String state, int expectedStatus, String expectedError) {
        ResponseEntity<Map<String, Object>> response = handler.handleDataAccess(
                new DataAccessResourceFailureException("fallo", new SQLException("causa", state)), request());

        assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).containsEntry("error", expectedError);
    }

    private HttpServletRequest request() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        given(request.getMethod()).willReturn("POST");
        given(request.getRequestURI()).willReturn("/api/soporte/tickets");
        return request;
    }
}
