package ec.edu.uteq.sga.infrastructure.config;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", ex.getStatusCode().value());
        body.put("error", ex.getStatusCode().toString());
        body.put("message", ex.getReason() != null ? ex.getReason() : "Error en la solicitud");
        body.put("mensaje", ex.getReason() != null ? ex.getReason() : "Error en la solicitud");
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Conflict");
        
        String msg = "No se pudo realizar la operación: el registro entra en conflicto con datos existentes o está en uso en otra parte del sistema.";
        String lower = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (lower.contains("unique") || lower.contains("duplicada") || lower.contains("key")) {
            msg = "Ya existe un registro con los mismos datos en el sistema (duplicado).";
        } else if (lower.contains("foreign key") || lower.contains("violates") || lower.contains("fk")) {
            msg = "No se puede eliminar o modificar este registro porque ya tiene datos vinculados (asistencias, actividades u horarios).";
        }
        body.put("message", msg);
        body.put("mensaje", msg);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        
        StringBuilder sb = new StringBuilder("Datos de entrada no válidos: ");
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            sb.append(error.getField()).append(" ").append(error.getDefaultMessage()).append("; ")
        );
        String msg = sb.toString();
        body.put("message", msg);
        body.put("mensaje", msg);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Unauthorized");
        body.put("message", "Usuario o contraseña incorrectos.");
        body.put("mensaje", "Usuario o contraseña incorrectos.");
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Internal Server Error");
        String msg = ex.getMessage() != null ? ex.getMessage() : "Ocurrió un error inesperado en el servidor.";
        body.put("message", msg);
        body.put("mensaje", msg);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
