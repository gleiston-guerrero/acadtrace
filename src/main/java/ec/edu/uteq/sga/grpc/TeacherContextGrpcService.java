package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignacion;
import ec.edu.uteq.sga.entity.Matricula;
import ec.edu.uteq.sga.grpc.contexto.*;
import ec.edu.uteq.sga.service.TeacherAuthorizationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.web.server.ResponseStatusException;

@GrpcService
@RequiredArgsConstructor
public class TeacherContextGrpcService extends TeacherContextServiceGrpc.TeacherContextServiceImplBase {

    private final TeacherAuthorizationService authService;

    @Override
    public void validateTeacherAssignment(ValidateAssignmentRequest request, StreamObserver<ValidateAssignmentResponse> responseObserver) {
        try {
            Asignacion asignacion = authService.validateTeacherAssignment(request.getIdDocente(), request.getIdAsignacion());
            ValidateAssignmentResponse response = ValidateAssignmentResponse.newBuilder()
                    .setIsValid(true)
                    .setIdAsignatura(asignacion.getAsignatura().getIdAsignatura())
                    .setIdGrado(asignacion.getGrado().getIdGrado())
                    .setIdAnoLectivo(asignacion.getAnoLectivo().getIdAnoLectivo())
                    .setIsActive(asignacion.isActivo())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error interno").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getStudentsByAssignment(StudentsByAssignmentRequest request, StreamObserver<StudentsByAssignmentResponse> responseObserver) {
        try {
            java.util.List<Matricula> matriculas = authService.getStudentsByAssignment(request.getIdAsignacion());
            StudentsByAssignmentResponse.Builder responseBuilder = StudentsByAssignmentResponse.newBuilder();
            for (Matricula matricula : matriculas) {
                ec.edu.uteq.sga.entity.Estudiante estudiante = matricula.getEstudiante();
                responseBuilder.addStudents(StudentProto.newBuilder()
                        .setIdEstudiante(estudiante.getIdEstudiante())
                        .setCedula(estudiante.getCedula() != null ? estudiante.getCedula() : "")
                        .setNombres(estudiante.getNombres() != null ? estudiante.getNombres() : "")
                        .setApellidos(estudiante.getApellidos() != null ? estudiante.getApellidos() : "")
                        .setIdMatricula(matricula.getIdMatricula())
                        .build());
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error interno").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void validateStudentEnrollment(ValidateEnrollmentRequest request, StreamObserver<ValidateEnrollmentResponse> responseObserver) {
        try {
            Matricula matricula = authService.validateStudentEnrollment(request.getIdMatricula(), request.getIdAsignacion());
            ValidateEnrollmentResponse response = ValidateEnrollmentResponse.newBuilder()
                    .setIsValid(true)
                    .setIdEstudiante(matricula.getEstudiante().getIdEstudiante())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error interno").withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getCurrentAcademicYear(EmptyRequest request, StreamObserver<AcademicYearResponse> responseObserver) {
        try {
            AnoLectivo ano = authService.getCurrentAcademicYear();
            AcademicYearResponse response = AcademicYearResponse.newBuilder()
                    .setIdAnoLectivo(ano.getIdAnoLectivo())
                    .setNombre(ano.getNombre())
                    .setFechaInicio(ano.getFechaInicio() != null ? ano.getFechaInicio().toString() : "")
                    .setFechaFin(ano.getFechaFin() != null ? ano.getFechaFin().toString() : "")
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResponseStatusException e) {
            responseObserver.onError(mapExceptionToGrpcStatus(e).asRuntimeException());
        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription("Error interno").withCause(e).asRuntimeException());
        }
    }

    private Status mapExceptionToGrpcStatus(ResponseStatusException e) {
        return switch (e.getStatusCode().value()) {
            case 401 -> Status.UNAUTHENTICATED.withDescription(e.getReason());
            case 403 -> Status.PERMISSION_DENIED.withDescription(e.getReason());
            case 404 -> Status.NOT_FOUND.withDescription(e.getReason());
            case 412 -> Status.FAILED_PRECONDITION.withDescription(e.getReason());
            case 400 -> Status.INVALID_ARGUMENT.withDescription(e.getReason());
            default -> Status.INTERNAL.withDescription(e.getReason());
        };
    }
}
