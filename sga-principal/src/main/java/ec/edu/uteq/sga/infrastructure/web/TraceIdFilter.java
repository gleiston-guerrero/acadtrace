package ec.edu.uteq.sga.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Toma el trace_id del header X-Trace-Id si el request ya viene de otro
 * microservicio (ej. secretaria propaga el suyo), o genera uno nuevo si es
 * el punto de entrada. Lo devuelve en la respuesta para que quien llamo
 * pueda seguir el rastro end-to-end.
 */
@Component
@Order(1)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        try {
            TraceContext.set(traceId);
            response.setHeader(HEADER, traceId);
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
