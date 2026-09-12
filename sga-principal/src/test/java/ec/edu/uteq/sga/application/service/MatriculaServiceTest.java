package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.matricula.MatriculaRequestDTO;
import ec.edu.uteq.sga.domain.dto.matricula.MatriculaResponseDTO;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: MatriculaService (SGA Principal)")
class MatriculaServiceTest {

    @Mock
    private MatriculaRepository matriculaRepo;

    @Mock
    private EstudianteRepository estudianteRepo;

    @Mock
    private GradoRepository gradoRepo;

    @Mock
    private ParaleloRepository paraleloRepo;

    @Mock
    private AnoLectivoRepository anoLectivoRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private MatriculaService matriculaService;

    private Estudiante estudiante;
    private Grado grado;
    private Paralelo paralelo;
    private AnoLectivo anoLectivo;
    private Matricula matricula;
    private MatriculaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        Representante rep = Representante.builder()
                .idRepresentante(1L)
                .cedula("1201112233")
                .nombres("Juan")
                .apellidos("Perez")
                .parentesco("PADRE")
                .telefonoPrincipal("0991234567")
                .build();

        estudiante = Estudiante.builder()
                .idEstudiante(10L)
                .cedula("1205316456")
                .nombres("Carlos")
                .apellidos("Castro")
                .representante(rep)
                .fechaNacimiento(LocalDate.of(2012, 5, 20))
                .build();

        grado = Grado.builder()
                .idGrado(1L)
                .nombre("Primer Año de Educación Básica")
                .build();

        paralelo = Paralelo.builder()
                .idParalelo(2L)
                .letra("A")
                .build();

        anoLectivo = AnoLectivo.builder()
                .idAnoLectivo(3L)
                .nombre("2026-2027")
                .esActual(true)
                .build();

        matricula = Matricula.builder()
                .idMatricula(100L)
                .estudiante(estudiante)
                .grado(grado)
                .paralelo(paralelo)
                .anoLectivo(anoLectivo)
                .numeroOrden((short) 1)
                .estado("ACTIVA")
                .observaciones("Matrícula ordinaria")
                .build();

        requestDTO = new MatriculaRequestDTO();
        requestDTO.setIdEstudiante(10L);
        requestDTO.setIdGrado(1L);
        requestDTO.setIdParalelo(2L);
        requestDTO.setIdAnoLectivo(3L);
        requestDTO.setEstado("ACTIVA");
        requestDTO.setObservaciones("Matrícula ordinaria");
    }

    // ─── 1. LISTADO Y BÚSQUEDA ───────────────────────────────────────────────
    @Test
    @DisplayName("1. Listar matrículas por estudiante — Retorna lista del estudiante")
    void listar_porEstudiante_retornaMatriculas() {
        given(matriculaRepo.findByEstudiante_IdEstudiante(10L)).willReturn(List.of(matricula));

        var pagina = matriculaService.listar(3L, 10L, 1L, null, 1, 10);

        assertThat(pagina).isNotNull();
        assertThat(pagina.items()).hasSize(1);
        assertThat(pagina.total()).isEqualTo(1);
        assertThat(pagina.items().get(0).getIdMatricula()).isEqualTo(100L);
    }

    @Test
    @DisplayName("2. Listar matrículas general por año lectivo con búsqueda de texto")
    void listar_generalPorAnoLectivoConQuery_retornaFiltradas() {
        given(matriculaRepo.findByAnoLectivoWithEstudiante(3L)).willReturn(List.of(matricula));

        var pagina = matriculaService.listar(3L, null, 1L, "Castro", 1, 10);

        assertThat(pagina).isNotNull();
        assertThat(pagina.items()).hasSize(1);
        assertThat(pagina.total()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. Obtener matrícula por ID existente — Retorna DTO")
    void obtener_whenExiste_retornaDTO() {
        given(matriculaRepo.findById(100L)).willReturn(Optional.of(matricula));

        MatriculaResponseDTO dto = matriculaService.obtener(100L);

        assertThat(dto).isNotNull();
        assertThat(dto.getIdMatricula()).isEqualTo(100L);
        assertThat(dto.getEstudianteNombres()).isEqualTo("Carlos");
        assertThat(dto.getGrado()).isEqualTo("Primer Año de Educación Básica");
    }

    @Test
    @DisplayName("4. Obtener matrícula por ID inexistente — Lanza 404 NOT_FOUND")
    void obtener_whenNoExiste_throwsNotFound() {
        given(matriculaRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> matriculaService.obtener(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── 2. CREACIÓN DE MATRÍCULA ───────────────────────────────────────────
    @Test
    @DisplayName("5. Crear matrícula — Datos válidos — Guarda y audita")
    void crear_whenDatosValidos_guardaYRetornaDTO() {
        given(estudianteRepo.findById(10L)).willReturn(Optional.of(estudiante));
        given(gradoRepo.findById(1L)).willReturn(Optional.of(grado));
        given(anoLectivoRepo.findById(3L)).willReturn(Optional.of(anoLectivo));
        given(paraleloRepo.findById(2L)).willReturn(Optional.of(paralelo));
        given(matriculaRepo.existsByEstudiante_IdEstudianteAndAnoLectivo_IdAnoLectivo(10L, 3L)).willReturn(false);
        given(matriculaRepo.findTopByAnoLectivo_IdAnoLectivoOrderByNumeroOrdenDesc(3L)).willReturn(Optional.empty());
        given(matriculaRepo.save(any(Matricula.class))).willReturn(matricula);

        MatriculaResponseDTO resultado = matriculaService.crear(requestDTO, null);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdMatricula()).isEqualTo(100L);
        then(matriculaRepo).should(times(1)).save(any(Matricula.class));
        then(auditoriaService).should(times(1)).registrarCrud(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("6. Crear matrícula — Estudiante ya matriculado en año — Lanza 409 CONFLICT")
    void crear_whenYaMatriculado_throwsConflict() {
        given(estudianteRepo.findById(10L)).willReturn(Optional.of(estudiante));
        given(gradoRepo.findById(1L)).willReturn(Optional.of(grado));
        given(anoLectivoRepo.findById(3L)).willReturn(Optional.of(anoLectivo));
        given(paraleloRepo.findById(2L)).willReturn(Optional.of(paralelo));
        given(matriculaRepo.existsByEstudiante_IdEstudianteAndAnoLectivo_IdAnoLectivo(10L, 3L)).willReturn(true);

        assertThatThrownBy(() -> matriculaService.crear(requestDTO, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(matriculaRepo).should(never()).save(any());
    }

    @Test
    @DisplayName("7. Crear matrícula — Paralelo nulo — Lanza 400 BAD_REQUEST")
    void crear_whenParaleloNulo_throwsBadRequest() {
        requestDTO.setIdParalelo(null);
        given(estudianteRepo.findById(10L)).willReturn(Optional.of(estudiante));
        given(gradoRepo.findById(1L)).willReturn(Optional.of(grado));
        given(anoLectivoRepo.findById(3L)).willReturn(Optional.of(anoLectivo));

        assertThatThrownBy(() -> matriculaService.crear(requestDTO, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    // ─── 3. CAMBIO DE ESTADO ────────────────────────────────────────────────
    @Test
    @DisplayName("8. Cambiar estado — Estado válido — Actualiza y audita")
    void cambiarEstado_whenValido_actualiza() {
        given(matriculaRepo.findById(100L)).willReturn(Optional.of(matricula));
        given(matriculaRepo.save(any(Matricula.class))).willReturn(matricula);

        matriculaService.cambiarEstado(100L, "RETIRADA", "Cambio de domicilio");

        assertThat(matricula.getEstado()).isEqualTo("RETIRADA");
        assertThat(matricula.getObservaciones()).isEqualTo("Cambio de domicilio");
        then(matriculaRepo).should(times(1)).save(matricula);
        then(auditoriaService).should(times(1)).registrarCrud(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("9. Cambiar estado — Estado inválido — Lanza 400 BAD_REQUEST")
    void cambiarEstado_whenInvalido_throwsBadRequest() {
        given(matriculaRepo.findById(100L)).willReturn(Optional.of(matricula));

        assertThatThrownBy(() -> matriculaService.cambiarEstado(100L, "ESTADO_INEXISTENTE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    // ─── 4. GENERACIÓN DE PDF ───────────────────────────────────────────────
    @Test
    @DisplayName("10. Generar PDF de Matrícula — Retorna arreglo de bytes no vacío")
    void generarPdfMatricula_whenValido_retornaBytes() {
        given(matriculaRepo.findById(100L)).willReturn(Optional.of(matricula));

        byte[] pdfBytes = matriculaService.generarPdfMatricula(100L);

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(0);
    }
}
