package ec.uteq.sga.soporte.application;

import ec.uteq.sga.soporte.application.service.TecnicoService;
import ec.uteq.sga.soporte.common.ApiException;
import ec.uteq.sga.soporte.infrastructure.grpc.TecnicoGrpcClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: TecnicoService (Filtrado gRPC de Técnicos)")
class TecnicoServiceTest {

    @Mock
    private TecnicoGrpcClient grpcClient;

    @InjectMocks
    private TecnicoService tecnicoService;

    @Test
    @DisplayName("1. Listar técnicos -- Filtra usuarios activos con rol SOPORTE_TECNICO o DIRECTOR")
    void listarTecnicos_filtersActiveTecnicosAndDirectores() {
        List<Map<String, Object>> mockUsers = List.of(
                Map.of("idUsuario", 1L, "username", "tec1", "activo", true, "roles", List.of("SOPORTE_TECNICO")),
                Map.of("idUsuario", 2L, "username", "dir1", "activo", true, "roles", List.of("DIRECTOR")),
                Map.of("idUsuario", 3L, "username", "doc1", "activo", true, "roles", List.of("DOCENTE")),
                Map.of("idUsuario", 4L, "username", "tec2_inactivo", "activo", false, "roles", List.of("SOPORTE_TECNICO"))
        );

        given(grpcClient.listarPorRol("")).willReturn(mockUsers);

        List<Map<String, Object>> result = tecnicoService.listarTecnicos();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(u -> (String) u.get("username"))
                .containsExactlyInAnyOrder("tec1", "dir1");
    }

    @Test
    @DisplayName("2. Listar técnicos -- Error de comunicación gRPC -- Lanza ApiException 500")
    void listarTecnicos_whenGrpcFails_throwsApiException() {
        given(grpcClient.listarPorRol("")).willThrow(new StatusRuntimeException(Status.UNAVAILABLE.withDescription("gRPC caído")));

        assertThatThrownBy(() -> tecnicoService.listarTecnicos())
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No se pudo consultar los técnicos en sga-principal");
    }
}
