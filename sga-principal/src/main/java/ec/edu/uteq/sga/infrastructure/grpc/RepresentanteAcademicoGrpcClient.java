package ec.edu.uteq.sga.infrastructure.grpc;

import ec.edu.uteq.sga.grpc.representante.*;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RepresentanteAcademicoGrpcClient {
    private static final Metadata.Key<String> INTERNAL_TOKEN =
            Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient("docente-service")
    private RepresentanteAcademicoServiceGrpc.RepresentanteAcademicoServiceBlockingStub stub;

    @Value("${app.grpc.internal-token}")
    private String internalToken;

    public CalificacionesResponse consultarCalificaciones(List<Long> matriculas) {
        return execute(() -> secured().consultarCalificaciones(
                MatriculasRequest.newBuilder().addAllIdMatriculas(matriculas).build()));
    }

    public AsistenciaResponse consultarAsistencia(List<Long> matriculas) {
        return execute(() -> secured().consultarAsistencia(
                MatriculasRequest.newBuilder().addAllIdMatriculas(matriculas).build()));
    }

    public ComunicadosResponse consultarComunicados(List<Long> asignaciones) {
        return execute(() -> secured().consultarComunicados(
                AsignacionesRequest.newBuilder().addAllIdAsignaciones(asignaciones).build()));
    }

    private RepresentanteAcademicoServiceGrpc.RepresentanteAcademicoServiceBlockingStub secured() {
        if (internalToken == null || internalToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Autenticación interna no configurada");
        }
        Metadata metadata = new Metadata();
        metadata.put(INTERNAL_TOKEN, internalToken);
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(5, TimeUnit.SECONDS);
    }

    private <T> T execute(GrpcCall<T> call) {
        try {
            return call.invoke();
        } catch (StatusRuntimeException error) {
            Status.Code code = error.getStatus().getCode();
            if (code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Servicio académico temporalmente no disponible");
            }
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Error controlado al consultar el servicio académico");
        }
    }

    @FunctionalInterface
    private interface GrpcCall<T> { T invoke(); }
}
