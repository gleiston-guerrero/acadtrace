package ec.uteq.sga.soporte.infrastructure.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Valida el "internal_token" de las llamadas gRPC entrantes al servidor de
 * sga-soporte (por ejemplo, sga-principal/docente/secretaria reportando una
 * incidencia via IncidenciaService). Mismo patron y mismo token de
 * desarrollo que usa sga-principal para validar sus propias llamadas
 * entrantes (ver InternalAuthInterceptor alla) -- asi el contrato de
 * autenticacion interna es el mismo en toda la malla de microservicios.
 */
@GrpcGlobalServerInterceptor
public class InternalAuthInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(InternalAuthInterceptor.class);

    private static final String INTERNAL_TOKEN_KEY = "internal_token";
    // TODO: mover a variable de entorno antes de producción real.
    @Value("${app.grpc.internal-token}")`r`n    private String expectedToken;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String token = headers.get(Metadata.Key.of(INTERNAL_TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER));

        if (token == null || !token.equals(expectedToken)) {
            log.warn("[grpc] Llamada rechazada: internal_token invalido o ausente (metodo {})",
                    call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing internal token"), headers);
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}
