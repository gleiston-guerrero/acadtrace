package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.domain.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.domain.dto.EstudianteListDTO;
import ec.edu.uteq.sga.domain.dto.RepresentanteInputDTO;
import ec.edu.uteq.sga.domain.entity.Estudiante;
import ec.edu.uteq.sga.domain.entity.Representante;
import ec.edu.uteq.sga.infrastructure.repository.EstudianteRepository;
import ec.edu.uteq.sga.infrastructure.repository.RepresentanteRepository;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: EstudianteService (SGA Principal)")
class EstudianteServiceTest {

    @Mock
    private EstudianteRepository estudianteRepo;

    @Mock
    private RepresentanteRepository representanteRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private EstudianteService estudianteService;

    private CrearEstudianteDTO dto;
    private Estudiante estudiante;
    private Representante representante;

    @BeforeEach
    void setUp() {
        dto = new CrearEstudianteDTO();
        dto.setCedula("1205316456");
        dto.setNombres("Carlos Leonardo");
        dto.setApellidos("Castro Lopez");
        dto.setCorreo("pcastrol2@uteq.edu.ec");
        dto.setFechaNacimiento(LocalDate.of(2005, 8, 15));
        dto.setGenero("MASCULINO");

        representante = new Representante();
        representante.setIdRepresentante(5L);
        representante.setCedula("1201112233");
        representante.setNombres("Pedro");
        representante.setApellidos("Castro");
        representante.setParentesco("PADRE");

        estudiante = new Estudiante();
        estudiante.setIdEstudiante(1L);
        estudiante.setCedula("1205316456");
        estudiante.setNombres("Carlos Leonardo");
        estudiante.setApellidos("Castro Lopez");
        estudiante.setCorreo("pcastrol2@uteq.edu.ec");
        estudiante.setEstado("ACTIVO");
        estudiante.setRepresentante(representante);
    }

    // ─── 1. CREACIÓN ────────────────────────────────────────────────────────
    @Test
    @DisplayName("1. Crear estudiante — Cédula única sin representante — Retorna DTO")
    void crear_whenCedulaYEmailUnicos_returnsDTO() {
        given(estudianteRepo.existsByCedula("1205316456")).willReturn(false);
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        EstudianteDetalleDTO result = estudianteService.crear(dto);

        assertThat(result).isNotNull();
        assertThat(result.getIdEstudiante()).isEqualTo(1L);
        assertThat(result.getCedula()).isEqualTo("1205316456");
        then(estudianteRepo).should(times(1)).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("2. Crear estudiante — Con datos de representante — Asocia y guarda")
    void crear_whenConRepresentante_asociaYGuarda() {
        RepresentanteInputDTO repDTO = new RepresentanteInputDTO();
        repDTO.setCedula("1201112233");
        repDTO.setNombres("Pedro");
        repDTO.setApellidos("Castro");
        repDTO.setParentesco("PADRE");
        dto.setRepresentante(repDTO);

        given(estudianteRepo.existsByCedula("1205316456")).willReturn(false);
        given(representanteRepo.findByCedula("1201112233")).willReturn(Optional.of(representante));
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        EstudianteDetalleDTO result = estudianteService.crear(dto);

        assertThat(result).isNotNull();
        then(representanteRepo).should(times(1)).findByCedula("1201112233");
        then(estudianteRepo).should(times(1)).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("3. Crear estudiante — Cédula duplicada — Lanza 409 CONFLICT")
    void crear_whenCedulaDuplicada_throwsConflicto() {
        given(estudianteRepo.existsByCedula("1205316456")).willReturn(true);

        assertThatThrownBy(() -> estudianteService.crear(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(estudianteRepo).should(never()).save(any());
    }

    // ─── 2. BÚSQUEDA Y OBTENCIÓN ─────────────────────────────────────────────
    @Test
    @DisplayName("4. Buscar por ID — Estudiante existe — Retorna DTO Detalle")
    void buscarPorId_whenExists_returnsDTO() {
        given(estudianteRepo.findByIdWithRepresentante(1L)).willReturn(Optional.of(estudiante));

        EstudianteDetalleDTO result = estudianteService.obtener(1L);

        assertThat(result).isNotNull();
        assertThat(result.getIdEstudiante()).isEqualTo(1L);
        assertThat(result.getCedula()).isEqualTo("1205316456");
    }

    @Test
    @DisplayName("5. Buscar por ID — No existe — Lanza 404 NOT_FOUND")
    void buscarPorId_whenNotExists_throwsNotFound() {
        given(estudianteRepo.findByIdWithRepresentante(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.obtener(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── 3. LISTADOS Y PAGINACIÓN ───────────────────────────────────────────
    @Test
    @DisplayName("6. Listar estudiantes — Sin filtro — Retorna lista completa")
    void listar_whenQueryNull_returnsTodos() {
        given(estudianteRepo.findAllWithRepresentante()).willReturn(List.of(estudiante));

        List<EstudianteListDTO> result = estudianteService.listar(null);

        assertThat(result).isNotEmpty();
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    @DisplayName("7. Listar estudiantes — Con filtro — Retorna filtrados")
    void listar_whenQueryText_returnsFiltrados() {
        given(estudianteRepo.searchWithRepresentante(anyString())).willReturn(List.of(estudiante));

        List<EstudianteListDTO> result = estudianteService.listar("Carlos");

        assertThat(result).isNotEmpty();
        assertThat(result.get(0).getNombres()).contains("Carlos");
    }
    @Test
    @DisplayName("8. Listar paginado — Retorna estructura de página correcta")
    void listarPaginado_whenValido_returnsPagina() {
        List<Estudiante> lista = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            Estudiante e = new Estudiante();
            e.setIdEstudiante((long) i);
            e.setCedula("120531645" + i);
            e.setNombres("Alumno " + i);
            e.setApellidos("Prueba");
            e.setEstado("ACTIVO");
            lista.add(e);
        }
        given(estudianteRepo.findAllWithRepresentante()).willReturn(lista);

        var pagina = estudianteService.listarPaginado(null, 1, 10);

        assertThat(pagina).isNotNull();
    }

    // ─── 4. BAJA LÓGICA Y ACTUALIZACIÓN ─────────────────────────────────────
    @Test
    @DisplayName("9. Cambiar estado — Desactivar — Marca estado INACTIVO")
    void cambiarEstado_whenExists_setInactivo() {
        given(estudianteRepo.findById(1L)).willReturn(Optional.of(estudiante));
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        estudianteService.cambiarEstado(1L, false);

        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        then(estudianteRepo).should().save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("INACTIVO");
    }

    @Test
    @DisplayName("10. Cambiar estado — ID inexistente — Lanza 404 NOT_FOUND")
    void cambiarEstado_whenNotExists_throwsNotFound() {
        given(estudianteRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> estudianteService.cambiarEstado(999L, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    @DisplayName("11. Actualizar estudiante — Datos válidos — Retorna DTO actualizado")
    void actualizar_whenValido_returnsActualizado() {
        given(estudianteRepo.findById(1L)).willReturn(Optional.of(estudiante));
        given(estudianteRepo.findByCedula("1205316456")).willReturn(Optional.of(estudiante));
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        dto.setNombres("Carlos Leonardo Modificado");

        EstudianteDetalleDTO result = estudianteService.actualizar(1L, dto);

        assertThat(result).isNotNull();
        assertThat(result.getIdEstudiante()).isEqualTo(1L);
        then(estudianteRepo).should(times(1)).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("12. Actualizar estudiante — Cédula cambiada a otra ya existente — Lanza 409 CONFLICT")
    void actualizar_whenCedulaPerteneceAOtro_throwsConflicto() {
        Estudiante otroEstudiante = new Estudiante();
        otroEstudiante.setIdEstudiante(2L);
        otroEstudiante.setCedula("1209999999");

        dto.setCedula("1209999999");

        given(estudianteRepo.findById(1L)).willReturn(Optional.of(estudiante));
        given(estudianteRepo.findByCedula("1209999999")).willReturn(Optional.of(otroEstudiante));

        assertThatThrownBy(() -> estudianteService.actualizar(1L, dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(estudianteRepo).should(never()).save(any());
    }
}