package ec.edu.uteq.sga.infrastructure.grpc;

import ec.edu.uteq.sga.infrastructure.grpc.actividades.*;
import ec.edu.uteq.sga.application.service.TeacherAuthorizationService;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ActividadGrpcClient {

    @GrpcClient("docente-service")
    private ActividadServiceGrpc.ActividadServiceBlockingStub stub;

    @Autowired
    private TeacherAuthorizationService authService;

    private ActividadServiceGrpc.ActividadServiceBlockingStub getStubWithMetadata() {
        String docenteId = authService.getAuthenticatedTeacher().getIdPersona().toString();

        io.grpc.Metadata metadata = new io.grpc.Metadata();
        metadata.put(io.grpc.Metadata.Key.of("docente_id", io.grpc.Metadata.ASCII_STRING_MARSHALLER), docenteId);
        metadata.put(io.grpc.Metadata.Key.of("internal_token", io.grpc.Metadata.ASCII_STRING_MARSHALLER), "***REMOVED***");
        return stub.withInterceptors(io.grpc.stub.MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    public ActividadResponse crearActividad(CrearActividadRequest request) {
        try {
            return getStubWithMetadata().crearActividad(request);
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC crearActividad: " + e.getMessage());
            throw e;
        }
    }

    public ActividadResponse editarActividad(EditarActividadRequest request) {
        try {
            return getStubWithMetadata().editarActividad(request);
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC editarActividad: " + e.getMessage());
            throw e;
        }
    }

    public ActividadResponse obtenerActividad(Long idActividad) {
        try {
            ObtenerActividadRequest request = ObtenerActividadRequest.newBuilder().setIdActividad(idActividad).build();
            return getStubWithMetadata().obtenerActividad(request);
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC obtenerActividad: " + e.getMessage());
            throw e;
        }
    }

    public List<ActividadDto> listarActividades(Long idAsignacion, Long idPeriodo) {
        try {
            ListarActividadesRequest.Builder builder = ListarActividadesRequest.newBuilder()
                    .setIdAsignacion(idAsignacion);
            if (idPeriodo != null) {
                builder.setIdPeriodo(idPeriodo);
            }
            ListarActividadesResponse response = getStubWithMetadata().listarActividades(builder.build());
            return response.getActividadesList();
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC listarActividades: " + e.getMessage());
            throw e;
        }
    }

    public EliminarActividadResponse eliminarActividad(Long idActividad) {
        try {
            EliminarActividadRequest request = EliminarActividadRequest.newBuilder().setIdActividad(idActividad).build();
            return getStubWithMetadata().eliminarActividad(request);
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC eliminarActividad: " + e.getMessage());
            throw e;
        }
    }
}
