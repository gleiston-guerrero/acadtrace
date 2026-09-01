package ec.uteq.sga.secretaria.infrastructure.common;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Toma X-Trace-Id del request si ya viene de otro punto de entrada (como HAProxy / Gateway),
 * o genera uno nuevo. Se propaga en TraceContext y en el MDC de SLF4J para logging estructurado JSON.
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_KEY = "traceId";
    public static final String MDC_SERVICE_KEY = "service";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = request.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        try {
            TraceContext.set(traceId);
            MDC.put(MDC_TRACE_KEY, traceId);
            MDC.put(MDC_SERVICE_KEY, "microservicio-secretaria");
            response.setHeader(HEADER, traceId);
            chain.doFilter(request, response);
        } finally {
            TraceContext.clear();
            MDC.clear();
        }
    }
}
