package ec.uteq.sga.secretaria.application.service;
import ec.edu.uteq.sga.grpc.principal.*;
import ec.uteq.sga.secretaria.domain.dto.GradoRequest;
import ec.uteq.sga.secretaria.infrastructure.grpc.PrincipalGrpcClient;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
class GradoServiceTest {
 private GradoProto grado(){return GradoProto.newBuilder().setIdGrado(2).setNombre("Octavo").setOrden(8).setActivo(true).setCapacidadMax(35).build();}
 private ParaleloProto paralelo(){return ParaleloProto.newBuilder().setIdParalelo(4).setIdGrado(2).setLetra("A").setActivo(true).setTotalEstudiantes(20).build();}
 @Test void listaGradosYParalelosTransformandoDatos(){PrincipalGrpcClient c=mock(PrincipalGrpcClient.class);GradoService s=new GradoService(c);when(c.listarGrados()).thenReturn(List.of(grado()));when(c.listarParalelos(null)).thenReturn(List.of(paralelo()));when(c.listarParalelos(2L)).thenReturn(List.of(paralelo()));Map<String,Object> item=s.listarTodos().get(0);assertThat(item.get("nombre")).isEqualTo("Octavo");assertThat(item.get("id_nivel")).isNull();assertThat(((List<?>)item.get("paralelos")).size()).isEqualTo(1);assertThat(s.listarParalelos(2L).get(0)).containsEntry("letra","A");}
 @Test void creaActualizaYCambiaEstadosConRequestGrpc(){PrincipalGrpcClient c=mock(PrincipalGrpcClient.class);GradoService s=new GradoService(c);GradoRequest dto=new GradoRequest("Noveno",null,null,null);when(c.crearGrado(any())).thenReturn(grado());when(c.actualizarGrado(any())).thenReturn(grado());when(c.crearParalelo(2,"B")).thenReturn(paralelo());assertThat(s.crear(dto)).containsEntry("nombre","Octavo");assertThat(s.actualizar(2,dto)).containsEntry("id_grado",2L);verify(c).crearGrado(argThat(r->r.getIdGrado()==0&&r.getOrden()==0&&r.getCapacidadMax()==0));verify(c).actualizarGrado(argThat(r->r.getIdGrado()==2));s.cambiarEstado(2,false);assertThat(s.crearParalelo(2,"B")).containsEntry("letra","A");s.cambiarEstadoParalelo(4,false);verify(c).cambiarEstadoGrado(2,false);verify(c).crearParalelo(2,"B");verify(c).cambiarEstadoParalelo(4,false);}
}
