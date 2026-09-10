package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.ActualizarHorasGradoRequest;
import ec.uteq.sga.secretaria.domain.dto.MallaRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MallaCurricularServiceTest {
    @Mock NamedParameterJdbcTemplate jdbc;
    private MallaCurricularService service() { return new MallaCurricularService(jdbc); }

    @Test
    void combinaMallaYAsignacionesSinDuplicarMaterias() {
        Map<String, Object> malla = Map.of("id_malla", 1L, "id_asignatura", 10L, "asignatura", "Mat", "codigo", "MAT", "horas_semana", 4, "dias_semana", 2, "duracion", 45);
        Map<String, Object> same = Map.of("id_asignatura", 10L, "asignatura", "Mat", "codigo", "MAT", "horas_semanales", 6, "docente_nombre", "Ana");
        Map<String, Object> onlyAssignment = Map.of("id_asignatura", 11L, "asignatura", "Fis", "codigo", "FIS", "horas_semanales", 3, "docente_nombre", "");
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(malla), List.of(same, onlyAssignment));

        Map<String, Object> result = service().porGrado(1, 2026);
        assertThat(result).containsEntry("totalHoras", 9);
        assertThat((List<?>) result.get("materias")).hasSize(2);
    }

    @Test
    void resumeHorasConElMayorValorEntreMallaYAsignacion() {
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(
                List.of(Map.of("id_grado", 1L, "id_asignatura", 2L, "horas", 4)),
                List.of(Map.of("id_grado", 1L, "id_asignatura", 2L, "horas", 6), Map.of("id_grado", 1L, "id_asignatura", 3L, "horas", 2)));

        assertThat(service().resumenGrados(2026).get(1L)).containsEntry("totalHoras", 8).containsEntry("cantMaterias", 2);
    }

    @Test
    void agregaYRechazaAsignaturaYaIncluida() {
        MallaRequest request = new MallaRequest(1L, 2L, 2026L, (short) 4, (short) 2, (short) 45);
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(0);
        service().agregar(request);
        verify(jdbc).update(contains("INSERT INTO"), any(MapSqlParameterSource.class));

        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class), eq(Integer.class))).thenReturn(1);
        assertThatThrownBy(() -> service().agregar(request)).isInstanceOf(ApiException.class).hasMessageContaining("registrada");
    }

    @Test
    void actualizaEliminaYProcesaCambiosDeHorasExistentesONuevos() {
        when(jdbc.update(contains("SET horas_semana"), any(MapSqlParameterSource.class))).thenReturn(1);
        service().actualizarHoras(4, (short) 5);
        when(jdbc.update(contains("DELETE FROM"), any(MapSqlParameterSource.class))).thenReturn(1);
        service().eliminar(4);

        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(99L), List.of());
        ActualizarHorasGradoRequest changes = new ActualizarHorasGradoRequest(1L, 2026L, List.of(
                Map.of("idAsignatura", 2L, "horasSemana", 5), Map.of("idAsignatura", 3L, "horasSemana", 4), Map.of("sin", "datos")));
        service().actualizarHorasGrado(changes);
        service().actualizarHorasGrado(new ActualizarHorasGradoRequest(1L, 2026L, List.of()));

        when(jdbc.update(contains("SET horas_semana"), any(MapSqlParameterSource.class))).thenReturn(0);
        when(jdbc.update(contains("DELETE FROM"), any(MapSqlParameterSource.class))).thenReturn(0);
        assertThatThrownBy(() -> service().actualizarHoras(5, (short) 3)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service().eliminar(5)).isInstanceOf(ApiException.class);
    }
}
