package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.EventoAcademicoRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CalendarioServiceTest {
    @Test
    void validaMesYAdministraEventos() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CatalogoService catalogo = mock(CatalogoService.class);
        CalendarioService service = new CalendarioService(jdbc, catalogo);
        assertThatThrownBy(() -> service.calendario("abril")).isInstanceOf(ApiException.class);

        when(catalogo.anosLectivos()).thenReturn(List.of());
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(), List.of(), List.of());
        assertThat(service.calendario("2026-04")).isNotEmpty();
        when(jdbc.query(anyString(), any(RowMapper.class))).thenReturn(List.of(Map.of("id_evento", 1L)));
        assertThat(service.listarEventos()).hasSize(1);
    }

    @Test
    void creaActualizaYEliminaEventoOIndicaInexistencia() {
        NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
        CalendarioService service = new CalendarioService(jdbc, mock(CatalogoService.class));
        EventoAcademicoRequest dto = new EventoAcademicoRequest("Examen", null, LocalDate.of(2026,4,2), null, "EXAMEN", null);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of(5L), List.of(Map.of("id_evento", 1L)), List.of(Map.of("id_evento", 1L)), List.of(Map.of("id_evento", 1L)));
        assertThat(service.crearEvento(dto, "ana")).containsEntry("id_evento", 1L);
        assertThat(service.actualizarEvento(1, dto)).containsEntry("id_evento", 1L);
        service.eliminarEvento(1);
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of());
        assertThatThrownBy(() -> service.eliminarEvento(2)).isInstanceOf(ApiException.class);
    }
}
