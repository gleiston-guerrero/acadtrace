package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.grpc.CalificacionRequest;
import ec.edu.uteq.sga.grpc.CalificacionesResponse;
import ec.edu.uteq.sga.grpc.DocenteServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Component;

@Component
public class DocenteGrpcClient {

    private final DocenteServiceGrpc.DocenteServiceBlockingStub stub;

    public DocenteGrpcClient() {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        this.stub = DocenteServiceGrpc.newBlockingStub(channel);
    }

    public CalificacionesResponse obtenerCalificaciones(
            Long idMatricula, Long idAsignacion, Integer trimestre) {
        CalificacionRequest request = CalificacionRequest.newBuilder()
                .setIdMatricula(idMatricula)
                .setIdAsignacion(idAsignacion)
                .setTrimestre(trimestre)
                .build();
        return stub.obtenerCalificaciones(request);
    }
}