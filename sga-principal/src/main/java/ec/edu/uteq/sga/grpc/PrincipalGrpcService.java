package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignatura;
import ec.edu.uteq.sga.entity.Grado;
import ec.edu.uteq.sga.entity.Paralelo;
import ec.edu.uteq.sga.grpc.principal.*;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.AsignaturaRepository;
import ec.edu.uteq.sga.repository.GradoRepository;
import ec.edu.uteq.sga.repository.ParaleloRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

/**
 * Servidor gRPC de sga-principal.
 * Expone el catalogo institucional (anos lectivos, grados, paralelos,
 * asignaturas) para que otros microservicios (Secretaria, Soporte, etc.) lo
 * consulten en vez de tocar la base de datos de sga_principal directamente.
 * Estudiantes/matriculas ya no se sirven aqui: son dominio de sga-secretaria.
 */
@GrpcService
@RequiredArgsConstructor
public class PrincipalGrpcService extends PrincipalServiceGrpc.PrincipalServiceImplBase {

    private final AnoLectivoRepository anoLectivoRepository;
    private final GradoRepository gradoRepository;
    private final ParaleloRepository paraleloRepository;
    private final AsignaturaRepository asignaturaRepository;

    @Override
    public void listarAnosLectivos(Empty request, StreamObserver<AnosLectivosResponse> responseObserver) {
        AnosLectivosResponse.Builder response = AnosLectivosResponse.newBuilder();
        List<AnoLectivo> anos = anoLectivoRepository.findAll();
        anos.forEach(a -> response.addAnosLectivos(toProto(a)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listarGrados(Empty request, StreamObserver<GradosResponse> responseObserver) {
        GradosResponse.Builder response = GradosResponse.newBuilder();
        gradoRepository.findAllByOrderByOrden().forEach(g -> response.addGrados(toProto(g)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listarParalelos(ListarParalelosRequest request, StreamObserver<ParalelosResponse> responseObserver) {
        ParalelosResponse.Builder response = ParalelosResponse.newBuilder();
        List<Paralelo> paralelos = request.getIdGrado() > 0
                ? paraleloRepository.findByGradoIdGradoOrderByLetra(request.getIdGrado())
                : paraleloRepository.findAll();
        paralelos.forEach(p -> response.addParalelos(toProto(p)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listarAsignaturas(Empty request, StreamObserver<AsignaturasResponse> responseObserver) {
        AsignaturasResponse.Builder response = AsignaturasResponse.newBuilder();
        asignaturaRepository.findAll().forEach(a -> response.addAsignaturas(toProto(a)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    private AnoLectivoProto toProto(AnoLectivo a) {
        return AnoLectivoProto.newBuilder()
                .setIdAnoLectivo(a.getIdAnoLectivo())
                .setNombre(nullToEmpty(a.getNombre()))
                .setFechaInicio(a.getFechaInicio() != null ? a.getFechaInicio().toString() : "")
                .setFechaFin(a.getFechaFin() != null ? a.getFechaFin().toString() : "")
                .setEsActual(a.isEsActual())
                .build();
    }

    private GradoProto toProto(Grado g) {
        return GradoProto.newBuilder()
                .setIdGrado(g.getIdGrado())
                .setNombre(nullToEmpty(g.getNombre()))
                .setOrden(g.getOrden() != null ? g.getOrden() : 0)
                .setActivo(g.isActivo())
                .build();
    }

    private ParaleloProto toProto(Paralelo p) {
        return ParaleloProto.newBuilder()
                .setIdParalelo(p.getIdParalelo())
                .setIdGrado(p.getGrado() != null ? p.getGrado().getIdGrado() : 0)
                .setLetra(nullToEmpty(p.getLetra()))
                .setActivo(p.isActivo())
                .build();
    }

    private AsignaturaProto toProto(Asignatura a) {
        return AsignaturaProto.newBuilder()
                .setIdAsignatura(a.getIdAsignatura())
                .setNombre(nullToEmpty(a.getNombre()))
                .setCodigo(nullToEmpty(a.getCodigo()))
                .setActiva(a.isActiva())
                .build();
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
