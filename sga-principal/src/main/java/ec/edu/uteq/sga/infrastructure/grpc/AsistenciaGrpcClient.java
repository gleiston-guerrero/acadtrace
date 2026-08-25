package ec.edu.uteq.sga.infrastructure.grpc;

import ec.edu.uteq.sga.infrastructure.grpc.asistencia.*;
import ec.edu.uteq.sga.application.service.TeacherAuthorizationService;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AsistenciaGrpcClient {

    @GrpcClient("docente-service")
    private AsistenciaServiceGrpc.AsistenciaServiceBlockingStub stub;

    @Autowired
    private TeacherAuthorizationService authService;

    private AsistenciaServiceGrpc.AsistenciaServiceBlockingStub getStubWithMetadata() {
        String docenteId = authService.getAuthenticatedTeacher().getIdPersona().toString();

        io.grpc.Metadata metadata = new io.grpc.Metadata();
        metadata.put(io.grpc.Metadata.Key.of("docente_id", io.grpc.Metadata.ASCII_STRING_MARSHALLER), docenteId);
        metadata.put(io.grpc.Metadata.Key.of("internal_token", io.grpc.Metadata.ASCII_STRING_MARSHALLER), "***REMOVED***");
        return stub.withInterceptors(io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    public AsistenciaListResponse registrarAsistenciaGrupal(RegistrarAsistenciaGrupalRequest request) {
        try {
            return getStubWithMetadata().registrarAsistenciaGrupal(request);
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }

    public AsistenciaResponse actualizarAsistencia(ActualizarAsistenciaRequest request) {
        try {
            return getStubWithMetadata().actualizarAsistencia(request);
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }

    public AsistenciaListResponse consultarAsistencia(ConsultarAsistenciaRequest request) {
        try {
            return getStubWithMetadata().consultarAsistencia(request);
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }

    public ResumenAsistenciaListResponse consultarResumenAsistencia(ConsultarResumenRequest request) {
        try {
            return getStubWithMetadata().consultarResumenAsistencia(request);
        } catch (StatusRuntimeException e) {
            throw e;
        }
    }
}
