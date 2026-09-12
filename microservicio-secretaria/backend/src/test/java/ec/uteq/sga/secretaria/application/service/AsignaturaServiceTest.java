package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.AsignaturaRequest;
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
class AsignaturaServiceTest {
    @Mock NamedParameterJdbcTemplate jdbc;
    private AsignaturaService service() { return new AsignaturaService(jdbc); }
    private AsignaturaRequest dto() { return new AsignaturaRequest("  Matematicas ", " MAT-1 ", " ", null); }

    @Test
    void listaYObtieneAsignaturasOIndicaAusencia() {
        Map<String, Object> row = Map.of("id_asignatura", 3L, "nombre", "Matematicas");
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(row));
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(row));
        assertThat(service().listarTodos()).containsExactly(row);
        assertThat(service().listarActivas()).containsExactly(row);
        assertThat(service().obtenerPorId(3)).isEqualTo(row);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service().obtenerPorId(4)).isInstanceOf(ApiException.class);
    }

    @Test
    void creaActualizaYCambiaEstadoConCamposNormalizados() {
        Map<String, Object> row = Map.of("id_asignatura", 3L);
        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(), List.of(row), List.of(row), List.of(), List.of(row));
        when(jdbc.queryForObject(contains("RETURNING"), any(MapSqlParameterSource.class), eq(Long.class))).thenReturn(3L);
        assertThat(service().crear(dto())).isEqualTo(row);
        assertThat(service().actualizar(3, dto())).isEqualTo(row);
        service().cambiarEstado(3, false);
        verify(jdbc).update(contains("activa = :activo"), any(MapSqlParameterSource.class));
    }

    @Test
    void rechazaDuplicadosAlCrearYActualizar() {
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(9L));
        assertThatThrownBy(() -> service().crear(dto())).isInstanceOf(ApiException.class).hasMessageContaining("existe");

        when(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(Map.of("id_asignatura", 3L)));
        assertThatThrownBy(() -> service().actualizar(3, dto())).isInstanceOf(ApiException.class).hasMessageContaining("existe");
    }
}
