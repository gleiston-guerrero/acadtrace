package ec.uteq.sga.soporte.election;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: Tareas Programadas con Elección de Líder (TicketScheduledTasks)")
class TicketScheduledTasksTest {

    @Mock
    private LeaderElectionService leaderElection;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @InjectMocks
    private TicketScheduledTasks scheduledTasks;

    @Test
    @DisplayName("1. cerrarTicketsInactivos -- Si no es líder, no interactúa con la base de datos")
    void cerrarTicketsInactivos_whenNotLeader_doesNothing() {
        given(leaderElection.isLeader()).willReturn(false);

        scheduledTasks.cerrarTicketsInactivos();

        verify(jdbc, never()).queryForList(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("2. cerrarTicketsInactivos -- Si es líder, ejecuta la consulta de cierre automático")
    void cerrarTicketsInactivos_whenLeader_executesQuery() {
        given(leaderElection.isLeader()).willReturn(true);
        given(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .willReturn(List.of(Map.of("numero_ticket", "TCK-001")));
        given(leaderElection.instanceId()).willReturn("soporte-instancia-1");

        scheduledTasks.cerrarTicketsInactivos();

        verify(jdbc).queryForList(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("3. cerrarTicketsInactivos -- Si es líder pero no hay tickets inactivos, no falla")
    void cerrarTicketsInactivos_whenLeaderAndNoTickets_completesSilently() {
        given(leaderElection.isLeader()).willReturn(true);
        given(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .willReturn(Collections.emptyList());

        scheduledTasks.cerrarTicketsInactivos();

        verify(jdbc).queryForList(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("4. enviarRecordatorios -- Si no es líder, no interactúa con la base de datos")
    void enviarRecordatorios_whenNotLeader_doesNothing() {
        given(leaderElection.isLeader()).willReturn(false);

        scheduledTasks.enviarRecordatorios();

        verify(jdbc, never()).queryForList(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("5. enviarRecordatorios -- Si es líder, ejecuta la consulta de tickets de alta prioridad")
    void enviarRecordatorios_whenLeader_executesQuery() {
        given(leaderElection.isLeader()).willReturn(true);
        given(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .willReturn(List.of(Map.of("numero_ticket", "TCK-002", "prioridad", "CRITICO")));
        given(leaderElection.instanceId()).willReturn("soporte-instancia-1");

        scheduledTasks.enviarRecordatorios();

        verify(jdbc).queryForList(anyString(), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("6. enviarRecordatorios -- Si es líder pero no hay tickets pendientes, no falla")
    void enviarRecordatorios_whenLeaderAndNoTickets_completesSilently() {
        given(leaderElection.isLeader()).willReturn(true);
        given(jdbc.queryForList(anyString(), any(SqlParameterSource.class)))
                .willReturn(Collections.emptyList());

        scheduledTasks.enviarRecordatorios();

        verify(jdbc).queryForList(anyString(), any(SqlParameterSource.class));
    }
}
