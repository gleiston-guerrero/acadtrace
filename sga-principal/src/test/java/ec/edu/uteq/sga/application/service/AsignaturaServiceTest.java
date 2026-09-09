package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.asignatura.AsignaturaRequestDTO;
import ec.edu.uteq.sga.domain.dto.asignatura.AsignaturaResponseDTO;
import ec.edu.uteq.sga.domain.entity.Asignatura;
import ec.edu.uteq.sga.infrastructure.repository.AsignaturaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: AsignaturaService (SGA Principal)")
class AsignaturaServiceTest {

    @Mock
    private AsignaturaRepository asignaturaRepo;

    @InjectMocks
    private AsignaturaService asignaturaService;

    private Asignatura asignatura;
    private AsignaturaRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        asignatura = Asignatura.builder()
                .idAsignatura(1L)
                .nombre("Matemática")
                .codigo("MAT-01")
                .descripcion("Matemática básica elemental")
                .horasSemanales((short) 5)
                .activa(true)
                .build();

        requestDTO = new AsignaturaRequestDTO();
        requestDTO.setNombre("Matemática");
        requestDTO.setCodigo("MAT-01");
        requestDTO.setDescripcion("Matemática básica elemental");
        requestDTO.setHorasSemanales((short) 5);
    }

    // ─── 1. LISTADO Y BÚSQUEDA ───────────────────────────────────────────────
    @Test
    @DisplayName("1. Listar todas las asignaturas — Retorna lista de DTOs")
    void listarTodos_retornaLista() {
        given(asignaturaRepo.findAll()).willReturn(List.of(asignatura));

        List<AsignaturaResponseDTO> lista = asignaturaService.listarTodos();

        assertThat(lista).isNotEmpty();
        assertThat(lista.size()).isEqualTo(1);
        assertThat(lista.get(0).getNombre()).isEqualTo("Matemática");
    }

    @Test
    @DisplayName("2. Listar asignaturas activas — Retorna solo activas")
    void listarActivos_retornaActivos() {
        given(asignaturaRepo.findByActivaTrue()).willReturn(List.of(asignatura));

        List<AsignaturaResponseDTO> lista = asignaturaService.listarActivos();

        assertThat(lista).isNotEmpty();
        assertThat(lista.get(0).isActivo()).isTrue();
    }

    @Test
    @DisplayName("3. Obtener por ID — Existe — Retorna DTO")
    void obtenerPorId_whenExiste_retornaDTO() {
        given(asignaturaRepo.findById(1L)).willReturn(Optional.of(asignatura));

        AsignaturaResponseDTO dto = asignaturaService.obtenerPorId(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getIdAsignatura()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("Matemática");
    }

    @Test
    @DisplayName("4. Obtener por ID — No existe — Lanza 404 NOT_FOUND")
    void obtenerPorId_whenNoExiste_throwsNotFound() {
        given(asignaturaRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> asignaturaService.obtenerPorId(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── 2. CREACIÓN ────────────────────────────────────────────────────────
    @Test
    @DisplayName("5. Crear asignatura — Datos válidos — Guarda y retorna DTO")
    void crear_whenValido_guardaYRetornaDTO() {
        given(asignaturaRepo.existsByNombre("Matemática")).willReturn(false);
        given(asignaturaRepo.existsByCodigo("MAT-01")).willReturn(false);
        given(asignaturaRepo.save(any(Asignatura.class))).willReturn(asignatura);

        AsignaturaResponseDTO result = asignaturaService.crear(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("Matemática");
        then(asignaturaRepo).should(times(1)).save(any(Asignatura.class));
    }

    @Test
    @DisplayName("6. Crear asignatura — Nombre duplicado — Lanza 409 CONFLICT")
    void crear_whenNombreDuplicado_throwsConflict() {
        given(asignaturaRepo.existsByNombre("Matemática")).willReturn(true);

        assertThatThrownBy(() -> asignaturaService.crear(requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(asignaturaRepo).should(never()).save(any());
    }

    @Test
    @DisplayName("7. Crear asignatura — Código duplicado — Lanza 409 CONFLICT")
    void crear_whenCodigoDuplicado_throwsConflict() {
        given(asignaturaRepo.existsByNombre("Matemática")).willReturn(false);
        given(asignaturaRepo.existsByCodigo("MAT-01")).willReturn(true);

        assertThatThrownBy(() -> asignaturaService.crear(requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(asignaturaRepo).should(never()).save(any());
    }

    // ─── 3. ACTUALIZACIÓN Y ESTADO ──────────────────────────────────────────
    @Test
    @DisplayName("8. Actualizar asignatura — Datos válidos — Actualiza campos")
    void actualizar_whenValido_actualiza() {
        given(asignaturaRepo.findById(1L)).willReturn(Optional.of(asignatura));
        given(asignaturaRepo.save(any(Asignatura.class))).willReturn(asignatura);

        requestDTO.setNombre("Matemática Avanzada");
        AsignaturaResponseDTO result = asignaturaService.actualizar(1L, requestDTO);

        assertThat(result).isNotNull();
        then(asignaturaRepo).should(times(1)).save(asignatura);
    }

    @Test
    @DisplayName("9. Cambiar estado — Desactivar asignatura")
    void cambiarEstado_actualizaEstado() {
        given(asignaturaRepo.findById(1L)).willReturn(Optional.of(asignatura));

        asignaturaService.cambiarEstado(1L, false);

        assertThat(asignatura.isActiva()).isFalse();
        then(asignaturaRepo).should(times(1)).save(asignatura);
    }
}
