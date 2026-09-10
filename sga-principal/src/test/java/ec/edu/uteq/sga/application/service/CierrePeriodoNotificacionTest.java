package ec.edu.uteq.sga.application.service;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CierrePeriodoNotificacionTest {
 @Mock EsquemaCalificacionRepository esquemaRepo; @Mock TipoAporteRepository aporteRepo; @Mock EscalaCalificacionesRepository escalaRepo; @Mock PeriodoEvaluacionRepository periodoRepo; @Mock AnoLectivoRepository anoRepo; @Mock NivelEducativoRepository nivelRepo; @Mock PushNotificationService push;
 @InjectMocks ConfiguracionCalificacionService service;
 @Test void noNotificaAntesDelCierreYCierraUnaSolaVez(){
  PeriodoEvaluacion p=PeriodoEvaluacion.builder().idPeriodo(3L).nombre("Primer trimestre").activo(true).build(); when(periodoRepo.findById(3L)).thenReturn(Optional.of(p)); when(periodoRepo.save(p)).thenReturn(p);
  verifyNoInteractions(push); assertThat(service.cerrarPeriodo(3L).getActivo()).isFalse(); service.cerrarPeriodo(3L); verify(push,times(1)).cierre(p);
 }
}
