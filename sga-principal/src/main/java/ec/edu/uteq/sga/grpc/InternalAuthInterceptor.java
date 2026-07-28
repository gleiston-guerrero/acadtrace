package ec.edu.uteq.sga.grpc;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;

@GrpcGlobalServerInterceptor
public class InternalAuthInterceptor implements ServerInterceptor {

    private static final String INTERNAL_TOKEN_KEY = "internal_token";
    // TODO: Usar variable de entorno para producción, hardcodeado por ahora como "***REMOVED***"
    private static final String EXPECTED_TOKEN = "***REMOVED***";

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {

        String token = headers.get(Metadata.Key.of(INTERNAL_TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER));

        if (token == null || !token.equals(EXPECTED_TOKEN)) {
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid or missing internal token"), headers);
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }
}
