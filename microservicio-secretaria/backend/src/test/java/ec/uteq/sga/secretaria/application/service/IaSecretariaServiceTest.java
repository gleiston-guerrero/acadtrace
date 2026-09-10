package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.*;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IaSecretariaServiceTest {
    private IaSecretariaService service() { return new IaSecretariaService(mock(NamedParameterJdbcTemplate.class)); }
    private IaDiagnosticoRequest request(double p, double a) { return new IaDiagnosticoRequest(1L,"Ana","8vo","A","Mat",1,p,a,Map.of()); }
    @Test void diagnosticosCubrenEscalasYRiesgos() {
        assertThat(service().generarDiagnostico(request(9.5,95)).nivelRiesgo()).isEqualTo("BAJO");
        assertThat(service().generarDiagnostico(request(7.5,89)).nivelRiesgo()).isEqualTo("MEDIO");
        var high=service().generarDiagnostico(request(4.5,80));
        assertThat(high.nivelRiesgo()).isEqualTo("ALTO"); assertThat(high.alertaRepresentante()).isTrue(); assertThat(high.escalaCualitativa()).contains("NAAR");
    }
    @Test void chatYCitacionUsanLasRamasSemanticasYValoresPorDefecto() {
        assertThat(service().procesarConsultaChat(new IaChatRequest("como matricular",null)).get("respuesta").toString()).contains("Matr");
        assertThat(service().procesarConsultaChat(new IaChatRequest("nota loei",null)).get("respuesta").toString()).contains("LOEI");
        assertThat(service().procesarConsultaChat(new IaChatRequest("hola",null)).get("respuesta").toString()).contains("hola");
        var c=service().generarBorradorCitacion(new IaCitacionRequest(null,"Luis",null,null,null,null));
        assertThat(c).containsEntry("idMatricula",0L); assertThat(c.get("documentoCitacion").toString()).contains("Representante Legal");
    }
    @Test void diagnosticoPorMatriculaResuelveONotificaAusencia() {
        NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class); IaSecretariaService s=new IaSecretariaService(jdbc);
        when(jdbc.queryForList(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class))).thenReturn(List.of(Map.of("estudiante","Ana","grado","8vo","paralelo","A")));
        assertThat(s.diagnosticoPorMatricula(1L).promedio()).isEqualTo(8.5);
        when(jdbc.queryForList(anyString(), any(org.springframework.jdbc.core.namedparam.SqlParameterSource.class))).thenReturn(List.of());
        assertThatThrownBy(()->s.diagnosticoPorMatricula(2L)).isInstanceOf(ApiException.class);
    }
}
