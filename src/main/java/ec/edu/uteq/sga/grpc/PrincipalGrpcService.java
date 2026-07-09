package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.dto.estudiante.EstudianteResponseDTO;
import ec.edu.uteq.sga.grpc.principal.*;
import ec.edu.uteq.sga.service.EstudianteService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.web.server.ResponseStatusException;

/**
 * Servidor gRPC de sga-principal.
 * Otros microservicios (Secretaría, Soporte, etc.) llaman aquí en vez de
 * tocar la base de datos de sga_principal directamente.
 */
@GrpcService
@RequiredArgsConstructor
public class PrincipalGrpcService extends PrincipalServiceGrpc.PrincipalServiceImplBase {

    private final EstudianteService estudianteService;

    @Override
    public void listarEstudiantes(ListarEstudiantesRequest request,
                                   StreamObserver<ListarEstudiantesResponse> responseObserver) {
        ListarEstudiantesResponse.Builder response = ListarEstudiantesResponse.newBuilder();

        estudianteService.listarTodos().forEach(dto -> response.addEstudiantes(toProto(dto)));

        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerEstudiante(ObtenerEstudianteRequest request,
                                   StreamObserver<EstudianteProto> responseObserver) {
        try {
            EstudianteResponseDTO dto = estudianteService.obtenerPorId(request.getIdEstudiante());
            responseObserver.onNext(toProto(dto));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("Estudiante no encontrado: " + request.getIdEstudiante())
                    .asRuntimeException());
        }
    }

    private EstudianteProto toProto(EstudianteResponseDTO dto) {
        EstudianteProto.Builder builder = EstudianteProto.newBuilder()
                .setIdEstudiante(dto.getIdEstudiante())
                .setCedula(nullToEmpty(dto.getCedula()))
                .setCodigoEstudiante(nullToEmpty(dto.getCodigoEstudiante()))
                .setNombres(nullToEmpty(dto.getNombres()))
                .setApellidos(nullToEmpty(dto.getApellidos()))
                .setFechaNacimiento(dto.getFechaNacimiento() != null ? dto.getFechaNacimiento().toString() : "")
                .setGenero(nullToEmpty(dto.getGenero()))
                .setDireccion(nullToEmpty(dto.getDireccion()))
                .setTelefono(nullToEmpty(dto.getTelefono()))
                .setCorreo(nullToEmpty(dto.getCorreo()))
                .setDiscapacidad(dto.isDiscapacidad())
                .setTipoDiscapacidad(nullToEmpty(dto.getTipoDiscapacidad()))
                .setPorcentajeDisc(dto.getPorcentajeDisc() != null ? dto.getPorcentajeDisc() : 0)
                .setEstado(nullToEmpty(dto.getEstado()))
                .setRepresentante(nullToEmpty(dto.getRepresentante()));

        return builder.build();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
