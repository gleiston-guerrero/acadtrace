package ec.uteq.sga.soporte.common;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.format.DateTimeParseException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void devuelveErroresDeValidacionYFormatoConContrato422() {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new TicketForm(), "ticket");
        binding.rejectValue("titulo", "NotBlank", "El titulo es obligatorio");
        MethodArgumentNotValidException validation = new MethodArgumentNotValidException(null, binding);

        var validationResponse = handler.handleValidation(validation);
        assertThat(validationResponse.getStatusCode().value()).isEqualTo(422);
        @SuppressWarnings("unchecked")
        Map<String, Object> firstDetail = (Map<String, Object>) ((java.util.List<?>) validationResponse.getBody().get("detalles")).get(0);
        assertThat(firstDetail)
                .containsEntry("campo", "titulo");

        assertThat(handler.handleTypeMismatch(mock(MethodArgumentTypeMismatchException.class)).getStatusCode().value()).isEqualTo(422);
        assertThat(handler.handleUnreadable(mock(HttpMessageNotReadableException.class)).getStatusCode().value()).isEqualTo(422);
        assertThat(handler.handleDateParse(new DateTimeParseException("fecha", "x", 0)).getStatusCode().value()).isEqualTo(422);
    }

    @Test
    void conservaEstadosDeApiYConvierteErroresDeInfraestructuraEn500() {
        assertThat(handler.handleApiException(new ApiException(404, "No existe")).getBody())
                .containsEntry("error", "No existe").doesNotContainKey("detalle");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/soporte/tickets");
        var dataAccess = handler.handleDataAccess(new DataAccessResourceFailureException("sin conexion"), request);
        var generic = handler.handleGeneric(new IllegalStateException("fallo"), request);

        assertThat(dataAccess.getStatusCode().value()).isEqualTo(500);
        assertThat(dataAccess.getBody()).containsEntry("detalle", "sin conexion");
        assertThat(generic.getStatusCode().value()).isEqualTo(500);
        assertThat(generic.getBody()).containsEntry("detalle", "fallo");
    }

    private static class TicketForm {
        private String titulo;

        public String getTitulo() {
            return titulo;
        }

        public void setTitulo(String titulo) {
            this.titulo = titulo;
        }
    }
}
