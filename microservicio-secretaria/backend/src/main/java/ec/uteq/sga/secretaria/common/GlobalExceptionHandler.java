package ec.uteq.sga.secretaria.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLException;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Replica exactamente los shapes de error de errorHandler.js (Node) para no romper el frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, Object>> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return ResponseEntity.status(422).body(validationBody(detalles));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        List<Map<String, Object>> detalles = ex.getConstraintViolations().stream()
                .map(cv -> fieldError(lastNode(cv.getPropertyPath().toString()), cv.getMessage()))
                .toList();
        return ResponseEntity.status(422).body(validationBody(detalles));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.status(422).body(validationBody(List.of(fieldError(ex.getName(), "Valor inválido"))));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(422).body(validationBody(List.of(fieldError("body", "JSON inválido"))));
    }

    @ExceptionHandler(DateTimeParseException.class)
    public ResponseEntity<Map<String, Object>> handleDateParse(DateTimeParseException ex) {
        return ResponseEntity.status(422).body(validationBody(List.of(fieldError("fecha", "Fecha inválida"))));
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(errorBody(ex.getMessage(), null));
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, Object>> handleDataAccess(DataAccessException ex, HttpServletRequest req) {
        log.error("[ERROR] {} {} -> {}", req.getMethod(), req.getRequestURI(), ex.getMessage());

        SQLException sqlEx = extractSQLException(ex);
        if (sqlEx != null && sqlEx.getSQLState() != null) {
            String detalle = sqlEx instanceof PSQLException psqlEx && psqlEx.getServerErrorMessage() != null
                    ? psqlEx.getServerErrorMessage().getDetail()
                    : null;
            switch (sqlEx.getSQLState()) {
                case "23505":
                    return ResponseEntity.status(409).body(errorBody("Registro duplicado", detalle));
                case "23503":
                    return ResponseEntity.status(400).body(errorBody("Referencia inválida", detalle));
                case "22P02":
                    return ResponseEntity.status(400).body(errorBody("Tipo de dato inválido", sqlEx.getMessage()));
                default:
                    break;
            }
        }
        return ResponseEntity.status(500).body(errorBody("Error interno del servidor", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex, HttpServletRequest req) {
        log.error("[ERROR] {} {} -> {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("Error interno del servidor", ex.getMessage()));
    }

    private SQLException extractSQLException(DataAccessException ex) {
        Throwable cause = ex.getMostSpecificCause();
        return cause instanceof SQLException sqlEx ? sqlEx : null;
    }

    private String lastNode(String propertyPath) {
        int idx = propertyPath.lastIndexOf('.');
        return idx >= 0 ? propertyPath.substring(idx + 1) : propertyPath;
    }

    private Map<String, Object> fieldError(String campo, String mensaje) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("campo", campo);
        map.put("mensaje", mensaje);
        return map;
    }

    private Map<String, Object> validationBody(List<Map<String, Object>> detalles) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "Datos inválidos");
        body.put("detalles", detalles);
        return body;
    }

    private Map<String, Object> errorBody(String error, String detalle) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        if (detalle != null) body.put("detalle", detalle);
        return body;
    }
}
