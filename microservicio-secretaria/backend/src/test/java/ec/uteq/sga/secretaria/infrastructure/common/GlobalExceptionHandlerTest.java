package ec.uteq.sga.secretaria.infrastructure.common;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import java.time.format.DateTimeParseException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
class GlobalExceptionHandlerTest {
 static class Form {String nombre;public String getNombre(){return nombre;}public void setNombre(String x){nombre=x;}}
 @Test void validaTodosLosErrores422YApi(){var h=new GlobalExceptionHandler();var b=new BeanPropertyBindingResult(new Form(),"f");b.rejectValue("nombre","x","requerido");assertThat(h.handleValidation(new MethodArgumentNotValidException(null,b)).getStatusCode().value()).isEqualTo(422);var mismatch=mock(MethodArgumentTypeMismatchException.class);when(mismatch.getName()).thenReturn("id");assertThat(h.handleTypeMismatch(mismatch).getStatusCode().value()).isEqualTo(422);assertThat(h.handleUnreadable(mock(HttpMessageNotReadableException.class)).getStatusCode().value()).isEqualTo(422);assertThat(h.handleDateParse(new DateTimeParseException("x","x",0)).getStatusCode().value()).isEqualTo(422);assertThat(h.handleApiException(ApiException.notFound("ausente")).getStatusCode().value()).isEqualTo(404);}
 @Test void convierteErroresInfraestructuraYGenericosEn500(){var h=new GlobalExceptionHandler();HttpServletRequest r=mock(HttpServletRequest.class);when(r.getMethod()).thenReturn("GET");when(r.getRequestURI()).thenReturn("/x");assertThat(h.handleDataAccess(new DataAccessResourceFailureException("bd"),r).getBody()).containsEntry("detalle","bd");assertThat(h.handleGeneric(new IllegalStateException("fallo"),r).getBody()).containsEntry("detalle","fallo");}
}
