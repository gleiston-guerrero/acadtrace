package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.dto.grado.GradoRequestDTO;
import ec.edu.uteq.sga.dto.grado.GradoResponseDTO;
import ec.edu.uteq.sga.dto.grado.ParaleloDTO;
import ec.edu.uteq.sga.dto.matricula.MatriculaRequestDTO;
import ec.edu.uteq.sga.dto.matricula.MatriculaResponseDTO;
import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignatura;
import ec.edu.uteq.sga.entity.Representante;
import ec.edu.uteq.sga.grpc.principal.*;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.AsignaturaRepository;
import ec.edu.uteq.sga.repository.EstudianteRepository;
import ec.edu.uteq.sga.repository.RepresentanteRepository;
import ec.edu.uteq.sga.service.AuditoriaService;
import ec.edu.uteq.sga.service.EstudianteService;
import ec.edu.uteq.sga.service.GradoService;
import ec.edu.uteq.sga.service.MatriculaService;
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
    private final AsignaturaRepository asignaturaRepository;
    private final EstudianteService estudianteService;
    private final GradoService gradoService;
    private final RepresentanteRepository representanteRepository;
    private final EstudianteRepository estudianteRepository;
    private final AuditoriaService auditoriaService;
    private final MatriculaService matriculaService;

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
        gradoService.listarTodos().forEach(g -> response.addGrados(toProto(g)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void listarParalelos(ListarParalelosRequest request, StreamObserver<ParalelosResponse> responseObserver) {
        ParalelosResponse.Builder response = ParalelosResponse.newBuilder();
        gradoService.listarTodos().stream()
                .filter(g -> request.getIdGrado() <= 0 || request.getIdGrado() == g.getIdGrado())
                .flatMap(g -> g.getParalelos().stream())
                .forEach(p -> response.addParalelos(toProto(p)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void crearGrado(GuardarGradoRequest request, StreamObserver<GradoProto> responseObserver) {
        try {
            GradoResponseDTO creado = gradoService.crear(fromProto(request));
            responseObserver.onNext(toProto(creado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void actualizarGrado(GuardarGradoRequest request, StreamObserver<GradoProto> responseObserver) {
        try {
            GradoResponseDTO actualizado = gradoService.actualizar(request.getIdGrado(), fromProto(request));
            responseObserver.onNext(toProto(actualizado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void cambiarEstadoGrado(CambiarEstadoGradoRequest request, StreamObserver<Empty> responseObserver) {
        try {
            gradoService.cambiarEstado(request.getIdGrado(), request.getActivo());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void crearParalelo(GuardarParaleloRequest request, StreamObserver<ParaleloProto> responseObserver) {
        try {
            ParaleloDTO creado = gradoService.crearParalelo(request.getIdGrado(), request.getLetra());
            responseObserver.onNext(toProto(creado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void cambiarEstadoParalelo(CambiarEstadoParaleloRequest request, StreamObserver<Empty> responseObserver) {
        try {
            gradoService.cambiarEstadoParalelo(request.getIdParalelo(), request.getActivo());
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    private GradoRequestDTO fromProto(GuardarGradoRequest r) {
        return GradoRequestDTO.builder()
                .nombre(r.getNombre())
                .orden((short) r.getOrden())
                .capacidadMax(r.getCapacidadMax() > 0 ? (short) r.getCapacidadMax() : 35)
                .idNivel(r.getIdNivel() > 0 ? r.getIdNivel() : null)
                .build();
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

    private GradoProto toProto(GradoResponseDTO d) {
        return GradoProto.newBuilder()
                .setIdGrado(d.getIdGrado())
                .setNombre(nullToEmpty(d.getNombre()))
                .setOrden(d.getOrden() != null ? d.getOrden() : 0)
                .setActivo(d.isActivo())
                .setCapacidadMax(d.getCapacidadMax() != null ? d.getCapacidadMax() : 0)
                .setIdNivel(d.getIdNivel() != null ? d.getIdNivel() : 0)
                .setNivelEducativo(nullToEmpty(d.getNivelEducativo()))
                .setTipoEscala(nullToEmpty(d.getTipoEscala()))
                .build();
    }

    private ParaleloProto toProto(ParaleloDTO p) {
        return ParaleloProto.newBuilder()
                .setIdParalelo(p.getIdParalelo())
                .setIdGrado(p.getIdGrado() != null ? p.getIdGrado() : 0)
                .setLetra(nullToEmpty(p.getLetra()))
                .setActivo(p.isActivo())
                .setTotalEstudiantes(p.getTotalEstudiantes())
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
            auditoriaService.registrarGrpcRecibida("estudiante", creado.getIdEstudiante(),
                    "Llamada gRPC recibida: crearEstudiante", "EXITO", null);
            responseObserver.onNext(toProto(creado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            auditoriaService.registrarGrpcRecibida("estudiante", null,
                    "Llamada gRPC recibida: crearEstudiante", "FALLO", e.getReason());
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void actualizarEstudiante(GuardarEstudianteRequest request, StreamObserver<EstudianteProto> responseObserver) {
        try {
            EstudianteDetalleDTO actualizado = estudianteService.actualizar(request.getIdEstudiante(), fromProto(request));
            auditoriaService.registrarGrpcRecibida("estudiante", actualizado.getIdEstudiante(),
                    "Llamada gRPC recibida: actualizarEstudiante", "EXITO", null);
            responseObserver.onNext(toProto(actualizado));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            auditoriaService.registrarGrpcRecibida("estudiante", request.getIdEstudiante(),
                    "Llamada gRPC recibida: actualizarEstudiante", "FALLO", e.getReason());
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

    // ---------------- Matriculas (gRPC) ----------------

    @Override
    public void crearMatricula(GuardarMatriculaRequest request, StreamObserver<MatriculaProto> responseObserver) {
        try {
            Long idUsuarioRegistro = request.getIdUsuarioRegistro() > 0 ? request.getIdUsuarioRegistro() : null;
            MatriculaResponseDTO creada = matriculaService.crear(fromProto(request), idUsuarioRegistro);
            auditoriaService.registrarGrpcRecibida("matricula", creada.getIdMatricula(),
                    "Llamada gRPC recibida: crearMatricula", "EXITO", null);
            responseObserver.onNext(toProto(creada));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            auditoriaService.registrarGrpcRecibida("matricula", null,
                    "Llamada gRPC recibida: crearMatricula", "FALLO", e.getReason());
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void obtenerMatricula(ObtenerMatriculaRequest request, StreamObserver<MatriculaProto> responseObserver) {
        try {
            responseObserver.onNext(toProto(matriculaService.obtener(request.getIdMatricula())));
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    @Override
    public void listarMatriculas(ListarMatriculasRequest request, StreamObserver<ListarMatriculasResponse> responseObserver) {
        MatriculaService.PaginaMatriculas pagina = matriculaService.listar(
                request.getIdAnoLectivo() > 0 ? request.getIdAnoLectivo() : null,
                request.getIdEstudiante() > 0 ? request.getIdEstudiante() : null,
                request.getQ(), request.getPage(), request.getLimit());
        ListarMatriculasResponse.Builder response = ListarMatriculasResponse.newBuilder().setTotal(pagina.total());
        pagina.items().forEach(d -> response.addMatriculas(toProto(d)));
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    @Override
    public void cambiarEstadoMatricula(CambiarEstadoMatriculaRequest request, StreamObserver<Empty> responseObserver) {
        try {
            matriculaService.cambiarEstado(request.getIdMatricula(), request.getEstado());
            auditoriaService.registrarGrpcRecibida("matricula", request.getIdMatricula(),
                    "Llamada gRPC recibida: cambiarEstadoMatricula", "EXITO", null);
            responseObserver.onNext(Empty.newBuilder().build());
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            auditoriaService.registrarGrpcRecibida("matricula", request.getIdMatricula(),
                    "Llamada gRPC recibida: cambiarEstadoMatricula", "FALLO", e.getReason());
            responseObserver.onError(toGrpcStatus(e).asRuntimeException());
        }
    }

    private MatriculaRequestDTO fromProto(GuardarMatriculaRequest r) {
        return MatriculaRequestDTO.builder()
                .idEstudiante(r.getIdEstudiante())
                .idGrado(r.getIdGrado())
                .idParalelo(r.getIdParalelo() > 0 ? r.getIdParalelo() : null)
                .idAnoLectivo(r.getIdAnoLectivo())
                .estado(r.getEstado().isBlank() ? null : r.getEstado())
                .observaciones(r.getObservaciones().isBlank() ? null : r.getObservaciones())
                .build();
    }

    private MatriculaProto toProto(MatriculaResponseDTO d) {
        return MatriculaProto.newBuilder()
                .setIdMatricula(d.getIdMatricula())
                .setIdEstudiante(d.getIdEstudiante())
                .setEstudianteNombres(nullToEmpty(d.getEstudianteNombres()))
                .setEstudianteApellidos(nullToEmpty(d.getEstudianteApellidos()))
                .setEstudianteCedula(nullToEmpty(d.getEstudianteCedula()))
                .setEstudianteCodigo(nullToEmpty(d.getEstudianteCodigo()))
                .setIdGrado(d.getIdGrado())
                .setIdParalelo(d.getIdParalelo() != null ? d.getIdParalelo() : 0)
                .setIdAnoLectivo(d.getIdAnoLectivo())
                .setNumeroOrden(d.getNumeroOrden() != null ? d.getNumeroOrden() : 0)
                .setFechaRegistro(d.getFechaRegistro() != null ? d.getFechaRegistro().toString() : "")
                .setEstado(nullToEmpty(d.getEstado()))
                .setObservaciones(nullToEmpty(d.getObservaciones()))
                .setRegistradoPor(nullToEmpty(d.getRegistradoPor()))
                .build();
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

    // ---------------- Representantes (gRPC) ----------------

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void obtenerRepresentante(ObtenerRepresentanteRequest request, StreamObserver<RepresentanteProto> responseObserver) {
        Representante r = representanteRepository.findById(request.getIdRepresentante())
                .orElse(null);
        if (r == null) {
            responseObserver.onError(Status.NOT_FOUND.withDescription("Representante no encontrado").asRuntimeException());
            return;
        }
        responseObserver.onNext(toRepresentanteProto(r));
        responseObserver.onCompleted();
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public void listarRepresentantesPorEstudiantes(ListarRepresentantesPorEstudiantesRequest request,
                                                    StreamObserver<RepresentantesResponse> responseObserver) {
        RepresentantesResponse.Builder resp = RepresentantesResponse.newBuilder();
        List<Long> ids = request.getIdEstudianteList();
        if (ids.isEmpty()) {
            responseObserver.onNext(resp.build());
            responseObserver.onCompleted();
            return;
        }
        // JOIN estudiante->representante (mirror local sga_principal.representantes)
        estudianteRepository.findAllById(ids).forEach(e -> {
            Representante r = e.getRepresentante();
            if (r != null) {
                resp.addItems(RepresentanteEstudianteProto.newBuilder()
                        .setIdEstudiante(e.getIdEstudiante())
                        .setRepresentante(toRepresentanteProto(r))
                        .build());
            }
        });
        responseObserver.onNext(resp.build());
        responseObserver.onCompleted();
    }

    private RepresentanteProto toRepresentanteProto(Representante r) {
        return RepresentanteProto.newBuilder()
                .setIdRepresentante(r.getIdRepresentante())
                .setCedula(nullToEmpty(r.getCedula()))
                .setNombres(nullToEmpty(r.getNombres()))
                .setApellidos(nullToEmpty(r.getApellidos()))
                .setParentesco(nullToEmpty(r.getParentesco()))
                .setTelefonoPrincipal(nullToEmpty(r.getTelefonoPrincipal()))
                .setTelefonoAlt(nullToEmpty(r.getTelefonoAlt()))
                .setCorreo(nullToEmpty(r.getCorreo()))
                .setDireccion(nullToEmpty(r.getDireccion()))
                .setFechaNacimiento(r.getFechaNacimiento() != null ? r.getFechaNacimiento().toString() : "")
                .setGenero(nullToEmpty(r.getGenero()))
                .setEstadoCivil(nullToEmpty(r.getEstadoCivil()))
                .setNacionalidad(nullToEmpty(r.getNacionalidad()))
                .setOcupacion(nullToEmpty(r.getOcupacion()))
                .setLugarTrabajo(nullToEmpty(r.getLugarTrabajo()))
                .setTelefonoTrabajo(nullToEmpty(r.getTelefonoTrabajo()))
                .setCargo(nullToEmpty(r.getCargo()))
                .setNivelInstruccion(nullToEmpty(r.getNivelInstruccion()))
                .setIngresoMensual(r.getIngresoMensual() != null ? r.getIngresoMensual().doubleValue() : 0.0)
                .setConviveConEstudiante(Boolean.TRUE.equals(r.getConviveConEstudiante()))
                .setContactoEmergenciaNombre(nullToEmpty(r.getContactoEmergenciaNombre()))
                .setContactoEmergenciaTelefono(nullToEmpty(r.getContactoEmergenciaTelefono()))
                .setObservaciones(nullToEmpty(r.getObservaciones()))
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
