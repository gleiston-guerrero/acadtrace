package ec.edu.uteq.sga.infrastructure.grpc;

import ec.edu.uteq.sga.grpc.representante.MatriculasRequest;
import ec.edu.uteq.sga.grpc.representante.RepresentanteAcademicoServiceGrpc;
import io.grpc.ClientInterceptor;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepresentanteAcademicoGrpcClientTest {
    @Test void unavailableSeTransformaEn503SinExponerStacktrace() {
        var stub = mock(RepresentanteAcademicoServiceGrpc.RepresentanteAcademicoServiceBlockingStub.class);
        when(stub.withInterceptors(any(ClientInterceptor[].class))).thenReturn(stub);
        when(stub.withDeadlineAfter(5, TimeUnit.SECONDS)).thenReturn(stub);
        when(stub.consultarCalificaciones(any(MatriculasRequest.class)))
                .thenThrow(Status.UNAVAILABLE.asRuntimeException());
        var client = new RepresentanteAcademicoGrpcClient();
        ReflectionTestUtils.setField(client, "stub", stub);
        ReflectionTestUtils.setField(client, "internalToken", "test-internal-token");
        assertThatThrownBy(() -> client.consultarCalificaciones(List.of(21L)))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("503");
    }
}
