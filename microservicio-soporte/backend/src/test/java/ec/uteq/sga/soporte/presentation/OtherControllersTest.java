package ec.uteq.sga.soporte.presentation;

import ec.uteq.sga.soporte.application.service.TecnicoService;
import ec.uteq.sga.soporte.election.LeaderElectionService;
import ec.uteq.sga.soporte.infrastructure.grpc.TecnicoGrpcClient;
import ec.uteq.sga.soporte.infrastructure.logging.MemoryLogAppender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: Controladores Auxiliares (Health, Election, Log, Tecnico, Usuario)")
class OtherControllersTest {

    @Mock
    private TecnicoService tecnicoService;

    @Mock
    private TecnicoGrpcClient tecnicoGrpcClient;

    @Mock
    private LeaderElectionService leaderElection;

    @Test
    @DisplayName("1. HealthController -- Retorna estado ok y servicio sga-soporte")
    void healthController_returnsOk() {
        HealthController healthController = new HealthController();
        Map<String, Object> health = healthController.health();

        assertThat(health).containsEntry("service", "sga-soporte");
        assertThat(health).containsEntry("status", "ok");
        assertThat(health).containsKey("timestamp");
    }

    @Test
    @DisplayName("2. TecnicoController -- Retorna lista de técnicos")
    void tecnicoController_returnsList() {
        TecnicoController controller = new TecnicoController(tecnicoService);
        given(tecnicoService.listarTecnicos()).willReturn(List.of(Map.of("username", "tec1")));

        List<Map<String, Object>> result = controller.listar();
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).containsEntry("username", "tec1");
    }

    @Test
    @DisplayName("3. UsuarioController -- Retorna técnicos y usuarios completos")
    void usuarioController_returnsData() {
        UsuarioController controller = new UsuarioController(tecnicoGrpcClient);
        given(tecnicoGrpcClient.listarTecnicos()).willReturn(List.of(Map.of("username", "tec1")));
        given(tecnicoGrpcClient.listarPorRol(null)).willReturn(List.of(Map.of("username", "usr1")));

        List<Map<String, Object>> tecnicos = controller.tecnicos();
        assertThat(tecnicos).hasSize(1);

        List<Map<String, Object>> usuarios = controller.usuarios();
        assertThat(usuarios).hasSize(1);
    }

    @Test
    @DisplayName("4. ElectionController -- Retorna estado de liderazgo Raft")
    void electionController_returnsStatus() {
        ElectionController controller = new ElectionController(leaderElection);
        given(leaderElection.instanceId()).willReturn("inst-1");
        given(leaderElection.isLeader()).willReturn(true);
        given(leaderElection.currentLeaderId()).willReturn("inst-1");

        Map<String, Object> status = controller.status();
        assertThat(status).containsEntry("instanceId", "inst-1");
        assertThat(status).containsEntry("isLeader", true);
        assertThat(status).containsEntry("currentLeaderId", "inst-1");
    }

    @Test
    @DisplayName("5. LogController -- Retorna logs dentro de límites")
    void logController_returnsLogs() {
        LogController controller = new LogController();
        List<Map<String, Object>> logs = controller.logsRecientes(10);
        assertThat(logs).isNotNull();
    }
}
