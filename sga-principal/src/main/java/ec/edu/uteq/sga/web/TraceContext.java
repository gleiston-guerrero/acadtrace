package ec.edu.uteq.sga.web;

import io.grpc.Context;

/**
 * Correlation ID del request actual, disponible tanto en el flujo HTTP
 * (TraceIdFilter puebla un ThreadLocal simple, correcto ahi porque el
 * filtro y el controller corren en el mismo hilo) como en el flujo gRPC
 * entrante (InternalAuthInterceptor usa io.grpc.Context en vez de un
 * ThreadLocal propio: grpc-java no garantiza que el metodo de servicio se
 * ejecute en el mismo hilo que interceptCall, pero si garantiza propagar
 * io.grpc.Context correctamente entre ambos).
 */
public final class TraceContext {

    public static final Context.Key<String> GRPC_TRACE = Context.key("trace_id");
    public static final Context.Key<String> GRPC_ACTOR = Context.key("actor_username");

    private static final ThreadLocal<String> HTTP_TRACE = new ThreadLocal<>();

    private TraceContext() {}

    public static void set(String traceId) {
        HTTP_TRACE.set(traceId);
    }

    /** Trace id del request actual: prioriza el de gRPC (io.grpc.Context) y cae al HTTP (ThreadLocal) si no aplica. */
    public static String current() {
        String grpcTrace = GRPC_TRACE.get();
        return grpcTrace != null ? grpcTrace : HTTP_TRACE.get();
    }

    /** Username que origino la llamada en el servicio de arrastre (ej. secretaria), solo presente en llamadas gRPC entrantes. */
    public static String actor() {
        return GRPC_ACTOR.get();
    }

    public static void clear() {
        HTTP_TRACE.remove();
    }
}
