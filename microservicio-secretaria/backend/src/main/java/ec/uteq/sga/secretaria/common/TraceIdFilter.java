package ec.uteq.sga.secretaria.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Toma X-Trace-Id del request si ya viene de otro punto de entrada, o
 * genera uno nuevo (secretaria es el punto de entrada mas comun para el
 * personal de secretaria). Se devuelve en la respuesta y queda disponible
 * en TraceContext durante todo el procesamiento del request.
 */
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
