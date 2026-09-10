package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HistorialServiceTest {
 private HistorialService service(NamedParameterJdbcTemplate j,CatalogoService c){return new HistorialService(j,mock(LamportClock.class),c);}
 @Test void historialBuscaAmbasFuentesYRechazaEstudianteAusente(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);CatalogoService c=mock(CatalogoService.class);when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(),List.of());assertThatThrownBy(()->service(j,c).historialEstudiante(1)).isInstanceOf(ApiException.class);}
 @Test void historialEnriqueceYOrdenaConCatalogo(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);CatalogoService c=mock(CatalogoService.class);Map<String,Object> student=Map.of("id_estudiante",1L);Map<String,Object> h=new HashMap<>(Map.of("id_grado_origen",2L,"id_ano_lectivo",3L));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenAnswer(i->{String sql=i.getArgument(0);return sql.contains("historial_promocion")?new ArrayList<>(List.of(h)):new ArrayList<>(List.of(student));});when(c.grados()).thenReturn(List.of(new CatalogoService.Grado(2,"Octavo",8,true)));when(c.anosLectivos()).thenReturn(List.of(new CatalogoService.AnoLectivo(3,"2026",LocalDate.of(2026,4,1),LocalDate.of(2027,3,1),true)));var result=service(j,c).historialEstudiante(1);assertThat(result.get("estudiante")).isEqualTo(student);Map<String,Object> historial=((List<Map<String,Object>>)result.get("historial")).get(0);assertThat(historial.get("grado")).isEqualTo("Octavo");assertThat(historial.get("ano_lectivo")).isEqualTo("2026");}
 @Test void listaPromocionesYResumenUsanCatalogo(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);CatalogoService c=mock(CatalogoService.class);Map<String,Object> row=new HashMap<>(Map.of("id_grado",2L,"id_paralelo",4L));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(row),List.of(Map.of("id_grado_origen",2L,"promovidos",4,"no_promovidos",1,"retirados",0,"promedio_general",8,"total_registrados",5)));when(c.grados()).thenReturn(List.of(new CatalogoService.Grado(2,"Octavo",8,true)));when(c.paralelos(any())).thenReturn(List.of(new CatalogoService.Paralelo(4,2,"A",true)));Map<String,Object> promocion=service(j,c).listarPromociones(3,2L,4L,"PROMOVIDO","ana").get(0);assertThat(promocion.get("grado")).isEqualTo("Octavo");assertThat(promocion.get("paralelo")).isEqualTo("A");Map<String,Object> resumen=service(j,c).resumenPromocion(3).get(0);assertThat(resumen.get("grado")).isEqualTo("Octavo");assertThat(resumen.get("promovidos")).isEqualTo(4);}
 @Test void eliminaRegistroOReportaAusencia(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);HistorialService s=service(j,mock(CatalogoService.class));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(Map.of("id_matricula",9L)));s.eliminarPromocion(1);verify(j,atLeast(3)).update(anyString(),any(MapSqlParameterSource.class));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of());assertThatThrownBy(()->s.eliminarPromocion(2)).isInstanceOf(ApiException.class);}
}
