package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
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
}
