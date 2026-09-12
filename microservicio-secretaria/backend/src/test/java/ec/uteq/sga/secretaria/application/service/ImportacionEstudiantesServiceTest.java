package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.domain.dto.ImportacionEstudianteRow;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.*;
import org.springframework.mock.web.MockMultipartFile;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImportacionEstudiantesServiceTest {
    @Test void parseaCsvYDetectaErroresDuplicadosYExistentes() {
        NamedParameterJdbcTemplate jdbc=mock(NamedParameterJdbcTemplate.class);
        when(jdbc.query(anyString(),any(MapSqlParameterSource.class),any(RowMapper.class))).thenReturn(List.of(),List.of(9L));
        ImportacionEstudiantesService s=new ImportacionEstudiantesService(jdbc,mock(EstudianteService.class),mock(MatriculaService.class));
        var f=new MockMultipartFile("f","alumnos.csv","text/csv",("Cédula,Nombres,Apellidos,Correo\n0102030405,Ana,Paz,a@u.edu.ec\n0102030405,Beto,Paz,\nabc,,,\n").getBytes());
        var r=s.parsearArchivo(f);
        assertThat(r.totalFilas()).isEqualTo(3); assertThat(r.filasConError()).isEqualTo(2); assertThat(r.estudiantes().get(1).getError()).contains("duplicada");
    }
    @Test void rechazaArchivosVaciosFormatosYEncabezadosInvalidos() {
        ImportacionEstudiantesService s=new ImportacionEstudiantesService(mock(NamedParameterJdbcTemplate.class),mock(EstudianteService.class),mock(MatriculaService.class));
        assertThatThrownBy(()->s.parsearArchivo(new MockMultipartFile("f",new byte[0]))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->s.parsearArchivo(new MockMultipartFile("f","x.txt","text/plain","x".getBytes()))).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->s.parsearArchivo(new MockMultipartFile("f","x.csv","text/csv","nombre\nAna".getBytes()))).isInstanceOf(ApiException.class);
    }
    @Test void confirmaFilasCreadasExistentesInvalidasYManejoDeMatricula() {
        EstudianteService estudiantes=mock(EstudianteService.class); MatriculaService matriculas=mock(MatriculaService.class);
        ImportacionEstudiantesService s=new ImportacionEstudiantesService(mock(NamedParameterJdbcTemplate.class),estudiantes,matriculas);
        ImportacionEstudianteRow nueva=new ImportacionEstudianteRow(); nueva.setCedula("0102030405"); nueva.setNombres("Ana"); nueva.setApellidos("Paz"); nueva.setCorreo("");
        ImportacionEstudianteRow existente=new ImportacionEstudianteRow(); existente.setYaExiste(true);
        ImportacionEstudianteRow invalida=new ImportacionEstudianteRow(); invalida.setError("cedula invalida");
        ImportacionEstudianteRow falla=new ImportacionEstudianteRow(); falla.setCedula("0203040506");falla.setNombres("Luis");falla.setApellidos("Lopez");
        when(estudiantes.crear(any(),eq("secretaria"))).thenReturn(Map.of("id_estudiante",7L)).thenThrow(ApiException.conflict("duplicado"));
        doThrow(ApiException.conflict("matricula")).when(matriculas).crear(any(),eq("secretaria"));
        Map<String,Object> resultado=s.confirmarImportacion(List.of(nueva,existente,invalida,falla),"secretaria",1L,2L,3L);
        assertThat(resultado).containsEntry("creados",1).containsEntry("existentes",1).containsEntry("omitidos",2).containsEntry("matriculados",0).containsEntry("total",4);
        verify(matriculas).crear(any(),eq("secretaria"));
    }
}
