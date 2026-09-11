package ec.edu.uteq.sga.infrastructure.grpc;

import ec.edu.uteq.sga.application.service.AuditoriaService;
import ec.edu.uteq.sga.infrastructure.web.TraceContext;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.beans.factory.annotation.Value;

/**
 * Valida el internal_token de las llamadas gRPC entrantes (de secretaria,
 * docente, etc.) y ademas: extrae el trace_id/actor de la metadata hacia
 * TraceContext (para que el servicio de negocio pueda auditar correlacionado
 * con el evento que lo origino en el otro microservicio) y audita los
 * intentos con token invalido -- antes esto fallaba en silencio.
 */
@GrpcGlobalServerInterceptor
public class InternalAuthInterceptor implements ServerInterceptor {

    private static final String INTERNAL_TOKEN_KEY = "internal_token";
    private static final String TRACE_ID_KEY = "trace_id";
    private static final String ACTOR_KEY = "actor_username";

    private final AuditoriaService auditoriaService;
    private final String expectedToken;

    public InternalAuthInterceptor(
            AuditoriaService auditoriaService,
            @Value("${app.grpc.internal-token}") String expectedToken) {
        this.auditoriaService = auditoriaService;
        this.expectedToken = expectedToken;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String traceId = headers.get(Metadata.Key.of(TRACE_ID_KEY, Metadata.ASCII_STRING_MARSHALLER));
        String actor = headers.get(Metadata.Key.of(ACTOR_KEY, Metadata.ASCII_STRING_MARSHALLER));
        String token = headers.get(Metadata.Key.of(INTERNAL_TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER));

        if (token == null || !token.equals(expectedToken)) {
            Context ctxFallo = Context.current().withValue(TraceContext.GRPC_TRACE, traceId);
            Context previo = ctxFallo.attach();
            try {
                auditoriaService.registrarFalloGrpcInterno(
                        "Llamada gRPC rechazada: internal_token invalido o ausente (metodo " + call.getMethodDescriptor().getFullMethodName() + ")");
            } finally {
                ctxFallo.detach(previo);
            }
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing internal token"), headers);
            return new ServerCall.Listener<>() {};
        }

        // io.grpc.Context (no un ThreadLocal propio) es lo que grpc-java garantiza propagar
        // correctamente hasta el metodo de servicio real, sin importar en que hilo/callback
        // termine ejecutandose (Contexts.interceptCall lo adjunta/desadjunta por nosotros).
        Context contextoConTrace = Context.current()
                .withValue(TraceContext.GRPC_TRACE, traceId)
                .withValue(TraceContext.GRPC_ACTOR, actor);
        return Contexts.interceptCall(contextoConTrace, call, headers, next);
    }
}
