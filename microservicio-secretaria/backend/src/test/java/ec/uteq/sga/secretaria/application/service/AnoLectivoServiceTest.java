package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.AnoLectivoRequest;
import ec.uteq.sga.secretaria.domain.dto.PeriodoEvaluacionRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnoLectivoServiceTest {
    @Mock NamedParameterJdbcTemplate jdbc;
    private AnoLectivoService service() { return new AnoLectivoService(jdbc); }
    private AnoLectivoRequest valid() { return new AnoLectivoRequest("  2026-2027  ", LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31)); }

    @Test
    void consultaAniosYReportaAusencias() {
        Map<String, Object> row = Map.of("id_ano_lectivo", 8L, "nombre", "2026-2027");
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(row));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(row));

        assertThat(service().listarTodos()).containsExactly(row);
        assertThat(service().obtenerActual()).isEqualTo(row);
        assertThat(service().obtenerPorId(8)).isEqualTo(row);

        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of());
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service().obtenerActual()).isInstanceOf(ApiException.class).hasMessageContaining("actual");
        assertThatThrownBy(() -> service().obtenerPorId(9)).isInstanceOf(ApiException.class).hasMessageContaining("no encontrado");
    }

    @Test
    void creaAnioYSusTresPeriodosPredeterminados() {
        Map<String, Object> created = Map.of("id_ano_lectivo", 4L);
        when(jdbc.queryForObject(contains("COUNT"), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        when(jdbc.queryForObject(contains("RETURNING id_ano_lectivo"), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(4L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(created));

        assertThat(service().crear(valid())).isEqualTo(created);
        verify(jdbc, times(3)).update(contains("periodos_evaluacion"), any(MapSqlParameterSource.class));
    }

    @Test
    void rechazaFechasInvalidasYNombresDuplicados() {
        AnoLectivoRequest invalid = new AnoLectivoRequest("2026", LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1));
        assertThatThrownBy(() -> service().crear(invalid)).isInstanceOf(ApiException.class).hasMessageContaining("fin");

        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(1);
        assertThatThrownBy(() -> service().crear(valid())).isInstanceOf(ApiException.class).hasMessageContaining("existe");
    }

    @Test
    void actualizaYEstableceElAnioActual() {
        Map<String, Object> found = Map.of("id_ano_lectivo", 5L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(found));
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        assertThat(service().actualizar(5, valid())).isEqualTo(found);
        service().establecerActual(5);
        verify(jdbc).update(contains("SET es_actual = false"), any(MapSqlParameterSource.class));
        verify(jdbc).update(contains("SET es_actual = true"), any(MapSqlParameterSource.class));
    }

    @Test
    void administraPeriodosIncluyendoValoresPredeterminadosYNoEncontrados() {
        Map<String, Object> ano = Map.of("id_ano_lectivo", 4L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(ano));
        when(jdbc.queryForObject(contains("RETURNING id_periodo"), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(22L);
        PeriodoEvaluacionRequest period = new PeriodoEvaluacionRequest("Extra", null, LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1), null);

        assertThat(service().listarPeriodos(4)).containsExactly(ano);
        assertThat(service().crearPeriodo(4, period)).containsEntry("id_periodo", 22L);
        when(jdbc.update(contains("UPDATE sga_docente.periodos"), any(MapSqlParameterSource.class))).thenReturn(1);
        when(jdbc.update(contains("DELETE FROM"), any(MapSqlParameterSource.class))).thenReturn(1);
        service().actualizarPeriodo(22, period);
        service().eliminarPeriodo(22);

        when(jdbc.update(contains("UPDATE sga_docente.periodos"), any(MapSqlParameterSource.class))).thenReturn(0);
        when(jdbc.update(contains("DELETE FROM"), any(MapSqlParameterSource.class))).thenReturn(0);
        assertThatThrownBy(() -> service().actualizarPeriodo(99, period)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service().eliminarPeriodo(99)).isInstanceOf(ApiException.class);
    }
}
