package ec.uteq.sga.soporte.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JdbcTicketRepositoryTest {

    @Mock private JdbcTemplate jdbc;
    private JdbcTicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcTicketRepository(jdbc);
    }

    @Test
    void consultasYReportes_deleganYTransformanResultados() {
        Map<String, Object> ticket = Map.of("id", 7L, "titulo", "Sin red");
        given(jdbc.queryForList(contains("ORDER BY id_ticket DESC"))).willReturn(List.of(ticket));
        given(jdbc.queryForList(contains("creado_por = ?"), eq("ana"))).willReturn(List.of(ticket));
        given(jdbc.queryForMap(contains("count(*) AS total"))).willReturn(Map.of(
                "total", 8L, "abiertos", 2L, "en_proceso", 3L, "resueltos", 2L, "cerrados", 1L));
        given(jdbc.queryForList(contains("GROUP BY categoria"))).willReturn(List.of(Map.of("categoria", "RED")));
        given(jdbc.queryForList(contains("GROUP BY asignado_a"))).willReturn(List.of(Map.of("tecnico", "ana")));
        given(jdbc.queryForList(contains("fecha_resolucion IS NOT NULL"))).willReturn(List.of(Map.of("totalTickets", 8L)));

        assertThat(repository.listarTodos()).containsExactly(ticket);
        assertThat(repository.listarPorCreador("ana")).containsExactly(ticket);
        assertThat(repository.estadisticas()).containsEntry("enProceso", 3L).containsEntry("cerrados", 1L);
        assertThat(repository.reportePorCategoria()).containsExactly(Map.of("categoria", "RED"));
        assertThat(repository.reportePorTecnico()).containsExactly(Map.of("tecnico", "ana"));
        assertThat(repository.reporteGeneral()).containsEntry("totalTickets", 8L);
    }

    @Test
    void buscarPorIdYReporteGeneral_manejanResultadosVacios() {
        given(jdbc.queryForList(contains("WHERE id_ticket = ?"), eq(99L))).willReturn(List.of());
        given(jdbc.queryForList(contains("fecha_resolucion IS NOT NULL"))).willReturn(List.of());

        assertThat(repository.buscarPorId(99L)).isEqualTo(Optional.empty());
        assertThat(repository.reporteGeneral()).containsEntry("totalTickets", 0).containsEntry("tiempoPromedioGlobal", 0);
    }

    @Test
    void buscarPorId_devuelvePrimerRegistroCuandoExiste() {
        Map<String, Object> ticket = Map.of("id", 10L);
        given(jdbc.queryForList(contains("WHERE id_ticket = ?"), eq(10L))).willReturn(List.of(ticket, Map.of("id", 11L)));

        assertThat(repository.buscarPorId(10L)).contains(ticket);
    }

    @Test
    void actualizarEstado_usaFechaResolucionSoloParaEstadosFinales() {
        repository.actualizarEstado(1L, "RESUELTO", "ana", "aplicado");
        repository.actualizarEstado(2L, "EN_PROCESO", null, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc, times(2)).update(sql.capture(), any(), any(), any(), any());
        assertThat(sql.getAllValues().get(0)).contains("fecha_resolucion = NOW()");
        assertThat(sql.getAllValues().get(1)).doesNotContain("fecha_resolucion");
    }

    @Test
    void actualizarPrioridadYAsignado_cubreCombinacionesValidasYNulas() {
        repository.actualizarPrioridadYAsignado(1L, "ALTA", "ana");
        repository.actualizarPrioridadYAsignado(2L, "BAJA", null);
        repository.actualizarPrioridadYAsignado(3L, null, "luis");
        repository.actualizarPrioridadYAsignado(4L, null, null);

        verify(jdbc).update(contains("prioridad = ?, asignado_a = ?"), eq("ALTA"), eq("ana"), eq(1L));
        verify(jdbc).update(contains("prioridad = ? WHERE"), eq("BAJA"), eq(2L));
        verify(jdbc).update(contains("asignado_a = ? WHERE"), eq("luis"), eq(3L));
        verifyNoMoreInteractions(jdbc);
    }

    @Test
    void historialYComentarios_deleganConLosParametrosDelDominio() {
        Map<String, Object> historial = Map.of("campo", "ESTADO");
        Map<String, Object> comentario = Map.of("contenido", "revisando");
        given(jdbc.queryForList(contains("historial_ticket"), eq(5L))).willReturn(List.of(historial));
        given(jdbc.queryForList(contains("comentarios"), eq(5L))).willReturn(List.of(comentario));

        repository.registrarHistorial(5L, "ESTADO", "ABIERTO", "RESUELTO", "ana");

        verify(jdbc).update(contains("historial_ticket"), eq(5L), eq("ESTADO"), eq("ABIERTO"), eq("RESUELTO"), eq("ana"));
        assertThat(repository.listarHistorial(5L)).containsExactly(historial);
        assertThat(repository.listarComentarios(5L)).containsExactly(comentario);
    }

    @Test
    void crearYAgregarComentario_retornaLasClavesGeneradas() {
        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id_ticket", 15L));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        long ticketId = repository.crear("TK-15", "Titulo", "Descripcion", "RED", "ALTA", "ana");

        doAnswer(invocation -> {
            KeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(Map.of("id_comentario", 23L));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
        Map<String, Object> comentario = repository.agregarComentario(15L, "ana", "Listo", true);

        assertThat(ticketId).isEqualTo(15L);
        assertThat(comentario).containsEntry("id", 23L).containsEntry("notaInterna", true);
    }
}
