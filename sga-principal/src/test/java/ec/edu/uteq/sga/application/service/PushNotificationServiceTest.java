package ec.edu.uteq.sga.application.service;
import ec.edu.uteq.sga.domain.dto.notificacion.EventoAcademicoPushDTO;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.firebase.FcmGateway;
import ec.edu.uteq.sga.infrastructure.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.*;
import jakarta.validation.constraints.Pattern;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {
 @Mock MatriculaRepository matriculas; @Mock AsignacionRepository asignaciones; @Mock DispositivoRepresentanteRepository dispositivos; @Mock EventoNotificacionPushRepository eventos; @Mock FcmGateway fcm;
 @InjectMocks PushNotificationService service;
 private final Usuario user=Usuario.builder().idUsuario(2L).build();
 private Matricula matricula(){Representante r=Representante.builder().usuario(user).build(); Estudiante e=Estudiante.builder().idEstudiante(5L).nombres("Ana").apellidos("Paz").representante(r).build();return Matricula.builder().idMatricula(7L).estudiante(e).build();}
 @Test void ausenciaGeneraUnaSolaVezYDesactivaTokenInvalido(){
  EventoAcademicoPushDTO dto=new EventoAcademicoPushDTO("ASISTENCIA:8:AUSENTE","AUSENTE",7L,null,3L,null,null,"Ana Paz registro una ausencia", LocalDate.now());
  DispositivoRepresentante device=DispositivoRepresentante.builder().token("oculto").usuario(user).activo(true).build();
  when(matriculas.findById(7L)).thenReturn(Optional.of(matricula())); when(eventos.findByClaveEventoAndUsuario_IdUsuario(dto.eventKey(),2L)).thenReturn(Optional.empty()); when(eventos.save(any())).thenAnswer(i->i.getArgument(0)); when(dispositivos.findByUsuario_IdUsuarioAndActivoTrue(2L)).thenReturn(List.of(device)); when(fcm.send(eq("oculto"),anyString(),anyString(),anyMap())).thenReturn(FcmGateway.Result.INVALID_TOKEN);
  service.procesar(dto); assertThat(device.isActivo()).isFalse(); verify(fcm).send(eq("oculto"),eq("Ausencia registrada"),anyString(),argThat(data -> "AUSENTE".equals(data.get("type"))));
 }
 @Test void eventoYaEnviadoNoDuplica(){
  EventoAcademicoPushDTO dto=new EventoAcademicoPushDTO("k","ATRASO",7L,null,null,null,null,"real",null);
  when(matriculas.findById(7L)).thenReturn(Optional.of(matricula())); when(eventos.findByClaveEventoAndUsuario_IdUsuario("k",2L)).thenReturn(Optional.of(EventoNotificacionPush.builder().claveEvento("k").usuario(user).estado("ENVIADO").build()));
  service.procesar(dto); verifyNoInteractions(fcm);
 }
 @Test void falloFcmQuedaRegistradoSinLanzarExcepcion(){
  EventoAcademicoPushDTO dto=new EventoAcademicoPushDTO("fallo","AUSENTE",7L,null,null,null,null,"real",null);
  DispositivoRepresentante device=DispositivoRepresentante.builder().token("oculto").usuario(user).activo(true).build(); EventoNotificacionPush event=EventoNotificacionPush.builder().claveEvento("fallo").usuario(user).build();
  when(matriculas.findById(7L)).thenReturn(Optional.of(matricula())); when(eventos.findByClaveEventoAndUsuario_IdUsuario("fallo",2L)).thenReturn(Optional.of(event)); when(dispositivos.findByUsuario_IdUsuarioAndActivoTrue(2L)).thenReturn(List.of(device)); when(fcm.send(anyString(),anyString(),anyString(),anyMap())).thenReturn(FcmGateway.Result.FAILED);
  assertThatCode(()->service.procesar(dto)).doesNotThrowAnyException(); assertThat(event.getEstado()).isEqualTo("FALLIDO");
 }
 @Test void contratoUsaElEstadoCanonicoAusente(){
  String regexp=Arrays.stream(EventoAcademicoPushDTO.class.getDeclaredConstructors()[0].getParameterAnnotations()[1])
    .filter(Pattern.class::isInstance).map(Pattern.class::cast).map(Pattern::regexp).findFirst().orElseThrow();
  assertThat("AUSENTE").matches(regexp);
  assertThat("AUSENCIA").doesNotMatch(regexp);
  assertThat("PRESENTE").doesNotMatch(regexp);
  assertThat("JUSTIFICADO").doesNotMatch(regexp);
 }
}
