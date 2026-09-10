package ec.edu.uteq.sga.application.service;
import ec.edu.uteq.sga.domain.dto.notificacion.DispositivoRequestDTO;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispositivoRepresentanteServiceTest {
 @Mock UsuarioRepository usuarios; @Mock RepresentanteRepository representantes; @Mock DispositivoRepresentanteRepository dispositivos;
 @InjectMocks DispositivoRepresentanteService service;
 @Test void registraTokenEnUsuarioAutenticadoYReutilizaDuplicado(){
  Usuario user=Usuario.builder().idUsuario(3L).username("madre").build();
  DispositivoRepresentante existente=DispositivoRepresentante.builder().idDispositivo(8L).token("token-valido").usuario(Usuario.builder().idUsuario(9L).build()).build();
  when(usuarios.findByUsername("madre")).thenReturn(Optional.of(user)); when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(Representante.builder().build())); when(dispositivos.findByToken("token-valido")).thenReturn(Optional.of(existente));
  service.registrar("madre",new DispositivoRequestDTO("token-valido","ANDROID"));
  assertThat(existente.getUsuario()).isSameAs(user); verify(dispositivos).save(existente);
 }
 @Test void usuarioSinRepresentanteNoPuedeAsociarToken(){
  when(usuarios.findByUsername("docente")).thenReturn(Optional.of(Usuario.builder().build())); when(representantes.findByUsuario_Username("docente")).thenReturn(Optional.empty());
  assertThatThrownBy(()->service.registrar("docente",new DispositivoRequestDTO("token","ANDROID"))).isInstanceOf(ResponseStatusException.class);
  verifyNoInteractions(dispositivos);
 }
}
