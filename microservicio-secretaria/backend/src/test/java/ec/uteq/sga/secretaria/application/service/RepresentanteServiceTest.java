package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.RepresentanteRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.security.CryptoService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepresentanteServiceTest {
 private RepresentanteRequest dto(){return new RepresentanteRequest("0102","Ana","Paz","Madre","099","", "a@x.ec","Calle",null,null,null,null,null,null,null,null,null,null,null,null,null,null);}
 private CryptoService crypto(){byte[] k=new byte[32];System.arraycopy("sga-secretaria-test-key-32bytes!".getBytes(),0,k,0,32);return new CryptoService(Base64.getEncoder().encodeToString(k));}
 @Test void listaObtieneYDescifraOReportaAusencia(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);var c=crypto();var s=new RepresentanteService(j,c,mock(AuditoriaService.class));Map<String,Object> row=new HashMap<>(Map.of("id_representante",1L,"direccion",c.encrypt("Calle"),"telefono_principal",c.encrypt("099")));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(row),List.of(row));assertThat(s.listarTodos("Ana").get(0)).containsEntry("direccion","Calle");assertThat(s.obtenerPorId(1)).containsEntry("telefono_principal","099");when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of());assertThatThrownBy(()->s.obtenerPorId(2)).isInstanceOf(ApiException.class);}
 @Test void creaActualizaYRechazaDuplicadoOFechaInvalida(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);AuditoriaService a=mock(AuditoriaService.class);var c=crypto();var s=new RepresentanteService(j,c,a);Map<String,Object> row=new HashMap<>(Map.of("id_representante",1L,"nombres","Ana","apellidos","Paz","direccion",c.encrypt("Calle"),"telefono_principal",c.encrypt("099")));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(),List.of(row),List.of(row),List.of(row));assertThat(s.crear(dto())).containsEntry("direccion","Calle");assertThat(s.actualizar(1,dto())).containsEntry("nombres","Ana");verify(a,times(2)).registrarCrud(anyString(),eq("representante"),anyLong(),anyString());when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(1L));assertThatThrownBy(()->s.crear(dto())).isInstanceOf(ApiException.class);assertThatThrownBy(()->s.crear(new RepresentanteRequest("", "A","B","","1","",null,"","mal",null,null,null,null,null,null,null,null,null,null,null,null,null))).isInstanceOf(ApiException.class);}
}
