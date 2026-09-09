package ec.uteq.sga.soporte.infrastructure.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JdbcTicketRepositoryTest {

    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);
    private final JdbcTicketRepository repository = new JdbcTicketRepository(jdbc);

    @Test
    void consultaListasBusquedaYReportesSinBaseDeDatos() {
        Map<String, Object> ticket = Map.of("id", 8L, "titulo", "Sin red");
        when(jdbc.queryForList(contains("ORDER BY id_ticket DESC"))).thenReturn(List.of(ticket));
        when(jdbc.queryForList(contains("creado_por"), eq("ana"))).thenReturn(List.of(ticket));
        when(jdbc.queryForList(contains("WHERE id_ticket"), eq(8L))).thenReturn(List.of(ticket));
        when(jdbc.queryForList(contains("GROUP BY categoria"))).thenReturn(List.of(Map.of("categoria", "Redes")));
        when(jdbc.queryForList(contains("GROUP BY asignado_a"))).thenReturn(List.of(Map.of("tecnico", "Ana")));
        when(jdbc.queryForList(contains("fecha_resolucion IS NOT NULL"))).thenReturn(List.of());

        assertThat(repository.listarTodos()).containsExactly(ticket);
        assertThat(repository.listarPorCreador("ana")).containsExactly(ticket);
        assertThat(repository.buscarPorId(8)).contains(ticket);
        assertThat(repository.reportePorCategoria()).hasSize(1);
        assertThat(repository.reportePorTecnico()).hasSize(1);
        assertThat(repository.reporteGeneral()).containsEntry("totalTickets", 0);

        when(jdbc.queryForList(contains("WHERE id_ticket"), eq(99L))).thenReturn(List.of());
        when(jdbc.queryForList(contains("fecha_resolucion IS NOT NULL"))).thenReturn(List.of(Map.of("total_tickets", 4)));
        assertThat(repository.buscarPorId(99)).isEmpty();
        assertThat(repository.reporteGeneral()).containsEntry("total_tickets", 4);
    }

    @Test
    void estadisticasEHistorialTransformanYDeleganResultados() {
        when(jdbc.queryForMap(anyString())).thenReturn(Map.of(
                "total", 5, "abiertos", 2, "en_proceso", 1, "resueltos", 1, "cerrados", 1));
        List<Map<String, Object>> historial = List.of(Map.of("campo", "estado"));
        List<Map<String, Object>> comentarios = List.of(Map.of("contenido", "Revisando"));
        when(jdbc.queryForList(contains("historial_ticket"), eq(8L))).thenReturn(historial);
        when(jdbc.queryForList(contains("comentarios"), eq(8L))).thenReturn(comentarios);

        assertThat(repository.estadisticas()).containsEntry("enProceso", 1).containsEntry("cerrados", 1);
        repository.registrarHistorial(8, "estado", "ABIERTO", "RESUELTO", "ana");
        assertThat(repository.listarHistorial(8)).isEqualTo(historial);
        assertThat(repository.listarComentarios(8)).isEqualTo(comentarios);
        verify(jdbc).update(contains("historial_ticket"), eq(8L), eq("estado"), eq("ABIERTO"), eq("RESUELTO"), eq("ana"));
    }

    @Test
    void crearYAgregarComentarioConstruyenStatementsYDevuelvenClavesGeneradas() throws Exception {
        prepararClaveGenerada(31L);
        assertThat(repository.crear("SUP-31", "Correo", "No envia", "Software", "ALTA", "ana")).isEqualTo(31L);

        prepararClaveGenerada(44L);
        assertThat(repository.agregarComentario(8, "ana", "Solucionado", true))
                .containsEntry("id", 44L)
                .containsEntry("idTicket", 8L)
                .containsEntry("notaInterna", true);
    }

    @Test
    void actualizarEstadoYPrioridadCubrenLasRamasDePersistencia() {
        repository.actualizarEstado(1, "RESUELTO", "ana", "reinicio");
        repository.actualizarEstado(1, "ABIERTO", null, null);
        repository.actualizarPrioridadYAsignado(1, "ALTA", "ana");
        repository.actualizarPrioridadYAsignado(1, "MEDIA", null);
        repository.actualizarPrioridadYAsignado(1, null, "luis");
        repository.actualizarPrioridadYAsignado(1, null, null);

        verify(jdbc).update(contains("fecha_resolucion = NOW"), eq("RESUELTO"), eq("ana"), eq("reinicio"), eq(1L));
        verify(jdbc).update(argThat(sql -> !sql.contains("fecha_resolucion = NOW")), eq("ABIERTO"), isNull(), isNull(), eq(1L));
        verify(jdbc).update(contains("prioridad = ?, asignado_a = ?"), eq("ALTA"), eq("ana"), eq(1L));
        verify(jdbc).update(contains("SET prioridad = ? WHERE"), eq("MEDIA"), eq(1L));
        verify(jdbc).update(contains("SET asignado_a = ? WHERE"), eq("luis"), eq(1L));
        verify(jdbc, times(5)).update(anyString(), any(Object[].class));
    }

    private void prepararClaveGenerada(long id) throws Exception {
        doAnswer(invocation -> {
            PreparedStatementCreator creator = invocation.getArgument(0);
            KeyHolder keyHolder = invocation.getArgument(1);
            Connection connection = mock(Connection.class);
            PreparedStatement statement = mock(PreparedStatement.class);
            when(connection.prepareStatement(anyString(), any(String[].class))).thenReturn(statement);
            creator.createPreparedStatement(connection);
            keyHolder.getKeyList().add(Map.of("id", id));
            return 1;
        }).when(jdbc).update(any(PreparedStatementCreator.class), any(KeyHolder.class));
    }
}
