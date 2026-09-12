package ec.uteq.sga.secretaria.application.service;
import ec.uteq.sga.secretaria.domain.dto.UsuarioRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class UsuarioServiceTest {
 private UsuarioRequest dto(){return new UsuarioRequest("ana","a@x.ec","Ana Maria","Paz","","","","","",List.of("SECRETARIA","INACTIVO"));}
 @Test void listaObtieneYRechazaAusencia(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);UsuarioService s=new UsuarioService(j);Map<String,Object> row=Map.of("id_usuario",1L,"nombres","Ana","username","ana");when(j.query(anyString(),any(RowMapper.class))).thenReturn(List.of(row));when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(row));assertThat(s.listarTodos()).containsExactly(row);assertThat(s.listarRoles()).containsExactly(row);assertThat(s.obtenerPorId(1)).isEqualTo(row);when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of());assertThatThrownBy(()->s.obtenerPorId(2)).isInstanceOf(ApiException.class);}
 @Test void creaRolesRestableceYActualizaEstado(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);UsuarioService s=new UsuarioService(j);Map<String,Object> row=Map.of("id_usuario",1L,"nombres","Ana Maria","username","ana");when(j.query(contains("username = :username OR correo"),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of());when(j.query(contains("WHERE u.id_usuario = :id"),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(row));when(j.query(contains("id_rol FROM sga_principal.roles"),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(3L));when(j.queryForObject(anyString(),any(MapSqlParameterSource.class),eq(Long.class))).thenReturn(1L);assertThat(s.crear(dto())).containsKey("temp_password");assertThat(s.resetearPassword(1)).containsKey("temp_password");s.cambiarEstado(1,false);s.asignarRoles(1,List.of("SECRETARIA"));verify(j,atLeast(3)).update(anyString(),any(MapSqlParameterSource.class));}
 @Test void rechazaUsuarioDuplicado(){NamedParameterJdbcTemplate j=mock(NamedParameterJdbcTemplate.class);UsuarioService s=new UsuarioService(j);when(j.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(1L));assertThatThrownBy(()->s.crear(dto())).isInstanceOf(ApiException.class);}
}
