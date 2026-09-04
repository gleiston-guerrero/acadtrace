package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import ec.edu.uteq.sga.infrastructure.grpc.RepresentanteAcademicoGrpcClient;
import ec.edu.uteq.sga.grpc.representante.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepresentanteConsultaServiceTest {
    @Mock RepresentanteRepository representantes;
    @Mock EstudianteRepository estudiantes;
    @Mock MatriculaRepository matriculas;
    @Mock AsignacionRepository asignaciones;
    @Mock RepresentanteAcademicoGrpcClient docente;
    @InjectMocks RepresentanteConsultaService service;

    private Representante representante() {
        return Representante.builder().idRepresentante(7L).build();
    }

    private Matricula matricula() {
        Estudiante estudiante = Estudiante.builder().idEstudiante(11L).nombres("Ana").apellidos("Paz").build();
        return Matricula.builder().idMatricula(21L).estudiante(estudiante)
                .grado(Grado.builder().nombre("Séptimo").build())
                .paralelo(Paralelo.builder().letra("A").build()).estado("ACTIVA").build();
    }

    @Test void listaSoloRepresentadosDelUsuario() {
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentante(7L)).thenReturn(List.of(matricula()));
        var respuesta = service.listar("madre");
        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).getIdEstudiante()).isEqualTo(11L);
        assertThat(respuesta.get(0).getMatriculas()).containsExactly(21L);
    }

    @Test void usuarioSinRepresentanteRecibeForbidden() {
        when(representantes.findByUsuario_Username("docente")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.listar("docente"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test void estudianteInexistenteRecibeNotFound() {
        when(estudiantes.existsById(999L)).thenReturn(false);
        assertThatThrownBy(() -> service.autorizar("madre", 999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test void estudianteAjenoRecibeForbidden() {
        when(estudiantes.existsById(99L)).thenReturn(true);
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentante(7L)).thenReturn(List.of(matricula()));
        assertThatThrownBy(() -> service.autorizar("madre", 99L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    @Test void autorizaHijoPropio() {
        when(estudiantes.existsById(11L)).thenReturn(true);
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentante(7L)).thenReturn(List.of(matricula()));
        assertThat(service.autorizar("madre", 11L).getMatriculas()).containsExactly(21L);
    }

    @Test void representanteAutorizadoObtieneCalificaciones() {
        autorizarMatriculaActiva();
        when(docente.consultarCalificaciones(List.of(21L))).thenReturn(CalificacionesResponse.newBuilder()
                .addCalificaciones(CalificacionRepresentante.newBuilder().setIdCalificacion(1).setNota(9.5).build())
                .build());
        assertThat((List<?>) service.calificaciones("madre", 11L).get("calificaciones")).hasSize(1);
    }

    @Test void estudianteAjenoNoInvocaGrpc() {
        when(estudiantes.existsById(99L)).thenReturn(true);
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentanteAndEstudiante(7L, 99L)).thenReturn(List.of());
        assertThatThrownBy(() -> service.calificaciones("madre", 99L)).hasMessageContaining("403");
        verifyNoInteractions(docente);
    }

    @Test void sinCalificacionesDevuelveListasVacias() {
        autorizarMatriculaActiva();
        when(docente.consultarCalificaciones(List.of(21L))).thenReturn(CalificacionesResponse.getDefaultInstance());
        assertThat((List<?>) service.calificaciones("madre", 11L).get("calificaciones")).isEmpty();
        assertThat((List<?>) service.calificaciones("madre", 11L).get("promedios")).isEmpty();
    }

    @Test void asistenciaVaciaDevuelveResumenCero() {
        autorizarMatriculaActiva();
        when(docente.consultarAsistencia(List.of(21L))).thenReturn(AsistenciaResponse.newBuilder()
                .setResumen(ResumenAsistenciaRepresentante.getDefaultInstance()).build());
        var respuesta = service.asistencia("madre", 11L);
        assertThat((List<?>) respuesta.get("asistencias")).isEmpty();
        assertThat(((java.util.Map<?, ?>) respuesta.get("resumen")).get("total")).isEqualTo(0);
    }

    @Test void comunicadosUsanSoloAsignacionesDeMatriculasActivas() {
        var m = matricula();
        m.setAnoLectivo(AnoLectivo.builder().idAnoLectivo(3L).build());
        m.getGrado().setIdGrado(4L);
        m.getParalelo().setIdParalelo(5L);
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentante(7L)).thenReturn(List.of(m));
        when(asignaciones.findByGrado_IdGradoAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivoAndActivoTrue(4L, 5L, 3L))
                .thenReturn(List.of(Asignacion.builder().idAsignacion(8L).build()));
        when(docente.consultarComunicados(List.of(8L))).thenReturn(ComunicadosResponse.newBuilder()
                .addComunicados(ComunicadoRepresentante.newBuilder().setId(9).setTitulo("Reunión").build()).build());
        assertThat(service.comunicados("madre")).extracting(item -> item.get("titulo")).containsExactly("Reunión");
    }

    private void autorizarMatriculaActiva() {
        when(estudiantes.existsById(11L)).thenReturn(true);
        when(representantes.findByUsuario_Username("madre")).thenReturn(Optional.of(representante()));
        when(matriculas.findActivasByRepresentanteAndEstudiante(7L, 11L)).thenReturn(List.of(matricula()));
    }
}
