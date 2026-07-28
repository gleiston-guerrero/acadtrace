package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignatura;
import ec.edu.uteq.sga.entity.Grado;
import ec.edu.uteq.sga.entity.Paralelo;
import ec.edu.uteq.sga.grpc.principal.*;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.AsignaturaRepository;
import ec.edu.uteq.sga.repository.GradoRepository;
import ec.edu.uteq.sga.repository.ParaleloRepository;
import ec.edu.uteq.sga.service.EstudianteService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * Servidor gRPC de sga-principal.
 * Expone el catalogo institucional (anos lectivos, grados, paralelos,
 * asignaturas) de solo lectura, y desde esta migracion tambien la escritura
 * de Estudiante (crear/actualizar) para que sga-secretaria deje de insertar
 * por SQL directo — centraliza el punto de escritura de esa tabla compartida.
 */
@GrpcService
@RequiredArgsConstructor
public class PrincipalGrpcService extends PrincipalServiceGrpc.PrincipalServiceImplBase {

    private final AnoLectivoRepository anoLectivoRepository;
    private final GradoRepository gradoRepository;
    private final ParaleloRepository paraleloRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final EstudianteService estudianteService;

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

    @Override
    public void crearEstudiante(GuardarEstudianteRequest request, StreamObserver<EstudianteProto> responseObserver) {
        try {
            EstudianteDetalleDTO creado = estudianteService.crear(fromProto(request));
            responseObserver.onNext(toProto(creado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void actualizarEstudiante(GuardarEstudianteRequest request, StreamObserver<EstudianteProto> responseObserver) {
        try {
            EstudianteDetalleDTO actualizado = estudianteService.actualizar(request.getIdEstudiante(), fromProto(request));
            responseObserver.onNext(toProto(actualizado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void listarEstudiantes(ListarEstudiantesRequest request, StreamObserver<ListarEstudiantesResponse> responseObserver) {
        EstudianteService.PaginaEstudiantes pagina = estudianteService.listarPaginado(request.getQ(), request.getPage(), request.getLimit());
        ListarEstudiantesResponse.Builder response = ListarEstudiantesResponse.newBuilder().setTotal(pagina.total());
        pagina.items().forEach(d -> response.addEstudiantes(toProto(d)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void obtenerEstudiante(ObtenerEstudianteRequest request, StreamObserver<EstudianteProto> responseObserver) {
        try {
            responseObserver.onNext(toProto(estudianteService.obtener(request.getIdEstudiante())));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void cambiarEstadoEstudiante(CambiarEstadoEstudianteRequest request, StreamObserver<Empty> responseObserver) {
        try {
            estudianteService.cambiarEstado(request.getIdEstudiante(), request.getActivo());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    private CrearEstudianteDTO fromProto(GuardarEstudianteRequest r) {
        CrearEstudianteDTO dto = new CrearEstudianteDTO();
        dto.setCedula(r.getCedula());
        dto.setNombres(r.getNombres());
        dto.setApellidos(r.getApellidos());
        dto.setFechaNacimiento(r.getFechaNacimiento().isBlank() ? null : LocalDate.parse(r.getFechaNacimiento()));
        dto.setGenero(r.getGenero());
        dto.setCorreo(r.getCorreo());
        dto.setDireccion(r.getDireccion());
        dto.setTelefono(r.getTelefono());
        dto.setDiscapacidad(r.getDiscapacidad());
        dto.setTipoDiscapacidad(r.getTipoDiscapacidad());
        dto.setPorcentajeDisc(r.getPorcentajeDisc() > 0 ? (short) r.getPorcentajeDisc() : null);
        dto.setIdRepresentante(r.getIdRepresentante() > 0 ? r.getIdRepresentante() : null);
        dto.setIdUsuarioCreador(r.getIdUsuarioCreador() > 0 ? r.getIdUsuarioCreador() : null);
        dto.setCodigoEstudiante(r.getCodigoEstudiante());
        dto.setNacionalidad(r.getNacionalidad());
        dto.setEtnia(r.getEtnia());
        dto.setLugarNacimiento(r.getLugarNacimiento());
        dto.setViveCon(r.getViveCon());
        dto.setNumerosHermanos(r.getNumerosHermanos() > 0 ? (short) r.getNumerosHermanos() : null);
        dto.setBeneficioSocial(r.getBeneficioSocial());
        dto.setCarnetConadis(r.getCarnetConadis());
        dto.setFotoUrl(r.getFotoUrl());
        return dto;
    }

    private EstudianteProto toProto(EstudianteDetalleDTO d) {
        var rep = d.getRepresentante();
        return EstudianteProto.newBuilder()
                .setIdEstudiante(d.getIdEstudiante())
                .setCedula(nullToEmpty(d.getCedula()))
                .setCodigoEstudiante(nullToEmpty(d.getCodigoEstudiante()))
                .setNombres(nullToEmpty(d.getNombres()))
                .setApellidos(nullToEmpty(d.getApellidos()))
                .setFechaNacimiento(d.getFechaNacimiento() != null ? d.getFechaNacimiento().toString() : "")
                .setGenero(nullToEmpty(d.getGenero()))
                .setCorreo(nullToEmpty(d.getCorreo()))
                .setDireccion(nullToEmpty(d.getDireccion()))
                .setTelefono(nullToEmpty(d.getTelefono()))
                .setDiscapacidad(d.isDiscapacidad())
                .setTipoDiscapacidad(nullToEmpty(d.getTipoDiscapacidad()))
                .setPorcentajeDisc(d.getPorcentajeDisc() != null ? d.getPorcentajeDisc() : 0)
                .setIdRepresentante(d.getIdRepresentante() != null ? d.getIdRepresentante() : 0)
                .setEstado(nullToEmpty(d.getEstado()))
                .setRepNombres(rep != null ? nullToEmpty(rep.getNombres()) : "")
                .setRepApellidos(rep != null ? nullToEmpty(rep.getApellidos()) : "")
                .setRepTelefono(rep != null ? nullToEmpty(rep.getTelefonoPrincipal()) : "")
                .setRepParentesco(rep != null ? nullToEmpty(rep.getParentesco()) : "")
                .setNacionalidad(nullToEmpty(d.getNacionalidad()))
                .setEtnia(nullToEmpty(d.getEtnia()))
                .setLugarNacimiento(nullToEmpty(d.getLugarNacimiento()))
                .setViveCon(nullToEmpty(d.getViveCon()))
                .setNumerosHermanos(d.getNumerosHermanos() != null ? d.getNumerosHermanos() : 0)
                .setBeneficioSocial(d.isBeneficioSocial())
                .setCarnetConadis(nullToEmpty(d.getCarnetConadis()))
                .setFotoUrl(nullToEmpty(d.getFotoUrl()))
                .build();
    }

    private Status toGrpcStatus(ResponseStatusException e) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        Status grpcStatus = switch (status) {
            case NOT_FOUND -> Status.NOT_FOUND;
            case CONFLICT -> Status.ALREADY_EXISTS;
            case BAD_REQUEST -> Status.INVALID_ARGUMENT;
            default -> Status.INTERNAL;
        };
        return grpcStatus.withDescription(e.getReason());
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
