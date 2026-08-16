package ec.uteq.sga.secretaria.common;

/**
 * Correlation ID del request HTTP actual (ThreadLocal), poblado por
 * TraceIdFilter. Lo usa AuditoriaService al escribir en
 * sga_principal.auditoria y PrincipalGrpcClient para reenviarlo como
 * metadata en las llamadas gRPC salientes hacia sga-principal, de forma
 * que ambos lados de la llamada queden correlacionados con el mismo
 * trace_id aunque esten en microservicios distintos.
 */
public final class TraceContext {

    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private TraceContext() {}

    public static void set(String traceId) {
        CURRENT.set(traceId);
    }

    public static String current() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
