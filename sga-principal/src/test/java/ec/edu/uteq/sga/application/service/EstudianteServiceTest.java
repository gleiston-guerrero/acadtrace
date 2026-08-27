package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.CrearEstudianteDTO;
import ec.edu.uteq.sga.domain.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.domain.entity.Estudiante;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

/**
 * Pruebas Unitarias de EstudianteService con Mockito y JUnit 5.
 * - No levanta Spring Boot ni base de datos real (ejecuta en < 15ms).
 * - Simula los repositorios con @Mock.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: EstudianteService (SGA Principal)")
class EstudianteServiceTest {

    // 1. Dobles de prueba (Mocks) para no tocar la base de datos real
    @Mock
    private EstudianteRepository estudianteRepo;

    @Mock
    private RepresentanteRepository representanteRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    // 2. Inyecta los Mocks automáticamente en la clase real bajo prueba
    @InjectMocks
    private EstudianteService estudianteService;

    private CrearEstudianteDTO dto;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        // Datos de prueba reutilizables
        dto = new CrearEstudianteDTO();
        dto.setCedula("1205316456");
        dto.setNombres("Carlos Leonardo");
        dto.setApellidos("Castro Mora");
        dto.setCorreo("pcastrol2@uteq.edu.ec");
        dto.setFechaNacimiento(LocalDate.of(2005, 8, 15));
        dto.setGenero("MASCULINO");

        estudiante = new Estudiante();
        estudiante.setIdEstudiante(1L);
        estudiante.setCedula("1205316456");
        estudiante.setNombres("Carlos Leonardo");
        estudiante.setApellidos("Castro Mora");
        estudiante.setCorreo("pcastrol2@uteq.edu.ec");
        estudiante.setEstado("ACTIVO");
    }

    // ─── TEST 1: Crear estudiante exitosamente ──────────────────────────────
    @Test
    @DisplayName("1. Crear estudiante — Cédula única — Debe retornar DTO con ID")
    void crear_whenCedulaYEmailUnicos_returnsDTO() {
        // GIVEN (Arrange / Preparar escenario)
        given(estudianteRepo.existsByCedula("1205316456")).willReturn(false);
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        // WHEN (Act / Ejecutar acción)
        EstudianteDetalleDTO result = estudianteService.crear(dto);

        // THEN (Assert / Verificar resultados)
        assertThat(result).isNotNull();
        assertThat(result.getIdEstudiante()).isEqualTo(1L);
        assertThat(result.getCedula()).isEqualTo("1205316456");
        assertThat(result.getNombres()).isEqualTo("Carlos Leonardo");

        // Verificar que se llamó al método save() exactamente 1 vez
        then(estudianteRepo).should(times(1)).save(any(Estudiante.class));
    }

    // ─── TEST 2: Crear con cédula duplicada ──────────────────────────────────
    @Test
    @DisplayName("2. Crear estudiante — Cédula duplicada — Debe lanzar CONFLICT (409)")
    void crear_whenCedulaDuplicada_throwsConflicto() {
        // GIVEN: El repositorio dice que ya existe esa cédula
        given(estudianteRepo.existsByCedula("1205316456")).willReturn(true);

        // WHEN / THEN: Debe arrojar excepción 409
        assertThatThrownBy(() -> estudianteService.crear(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        // Regla: Si hay conflicto, NUNCA se debe intentar guardar en BD
        then(estudianteRepo).should(never()).save(any());
    }

    // ─── TEST 3: Buscar por ID inexistente ───────────────────────────────────
    @Test
    @DisplayName("3. Buscar por ID — ID no existe — Debe lanzar NOT_FOUND (404)")
    void buscarPorId_whenNotExists_throwsNotFound() {
        // GIVEN: El repositorio retorna vacío (Optional.empty)
        given(estudianteRepo.findByIdWithRepresentante(999L)).willReturn(Optional.empty());

        // WHEN / THEN: Debe arrojar 404 NOT_FOUND
        assertThatThrownBy(() -> estudianteService.obtener(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── TEST 4: Baja lógica con ArgumentCaptor ─────────────────────────────
    @Test
    @DisplayName("4. Cambiar estado — Desactivar estudiante — Debe marcar estado INACTIVO")
    void cambiarEstado_whenExists_setInactivo() {
        // GIVEN
        given(estudianteRepo.findById(1L)).willReturn(Optional.of(estudiante));
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        // WHEN
        estudianteService.cambiarEstado(1L, false);

        // THEN: ArgumentCaptor captura el objeto enviado al save() para verificar que cambió a INACTIVO
        ArgumentCaptor<Estudiante> captor = ArgumentCaptor.forClass(Estudiante.class);
        then(estudianteRepo).should().save(captor.capture());
        assertThat(captor.getValue().getEstado()).isEqualTo("INACTIVO");
    }

    // ─── TEST 5: Actualizar estudiante exitosamente ──────────────────────────
    @Test
    @DisplayName("5. Actualizar estudiante — Datos válidos — Debe retornar DTO actualizado")
    void actualizar_whenValido_returnsActualizado() {
        // GIVEN
        given(estudianteRepo.findById(1L)).willReturn(Optional.of(estudiante));
        given(estudianteRepo.findByCedula("1205316456")).willReturn(Optional.of(estudiante));
        given(estudianteRepo.save(any(Estudiante.class))).willReturn(estudiante);

        dto.setNombres("Carlos Leonardo Modificado");

        // WHEN
        EstudianteDetalleDTO result = estudianteService.actualizar(1L, dto);

        // THEN
        assertThat(result).isNotNull();
        then(estudianteRepo).should(times(1)).save(any(Estudiante.class));
    }
}