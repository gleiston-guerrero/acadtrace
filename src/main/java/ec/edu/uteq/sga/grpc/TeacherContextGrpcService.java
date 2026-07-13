package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.grpc.contexto.*;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class TeacherContextGrpcService extends TeacherContextServiceGrpc.TeacherContextServiceImplBase {

    @Override
    public void validateTeacherAssignment(ValidateAssignmentRequest request, StreamObserver<ValidateAssignmentResponse> responseObserver) {
        // Implementación básica (Mock) para Fase A
        // Todo: Conectar con la lógica real de negocio usando EstudianteService y BD
        ValidateAssignmentResponse response = ValidateAssignmentResponse.newBuilder()
                .setIsValid(true)
                .setIdAsignatura(1)
                .setIdGrado(1)
                .setIdParalelo(1)
                .setIdAnoLectivo(1)
                .setIsActive(true)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getStudentsByAssignment(StudentsByAssignmentRequest request, StreamObserver<StudentsByAssignmentResponse> responseObserver) {
        StudentsByAssignmentResponse response = StudentsByAssignmentResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void validateStudentEnrollment(ValidateEnrollmentRequest request, StreamObserver<ValidateEnrollmentResponse> responseObserver) {
        ValidateEnrollmentResponse response = ValidateEnrollmentResponse.newBuilder()
                .setIsValid(true)
                .setIdEstudiante(1)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getCurrentAcademicYear(EmptyRequest request, StreamObserver<AcademicYearResponse> responseObserver) {
        AcademicYearResponse response = AcademicYearResponse.newBuilder()
                .setIdAnoLectivo(1)
                .setNombre("2026-2027")
                .setFechaInicio("2026-05-01")
                .setFechaFin("2027-02-28")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
