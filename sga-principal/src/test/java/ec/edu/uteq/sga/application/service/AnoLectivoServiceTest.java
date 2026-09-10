package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.anolectivo.AnoLectivoRequestDTO;
import ec.edu.uteq.sga.domain.dto.anolectivo.AnoLectivoResponseDTO;
import ec.edu.uteq.sga.domain.entity.AnoLectivo;
import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: AnoLectivoService (SGA Principal)")
class AnoLectivoServiceTest {

    @Mock
    private AnoLectivoRepository anoLectivoRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @InjectMocks
    private AnoLectivoService anoLectivoService;

    private AnoLectivo anoLectivo;
    private AnoLectivoRequestDTO requestDTO;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .username("admin")
                .correo("admin@uteq.edu.ec")
                .build();

        anoLectivo = AnoLectivo.builder()
                .idAnoLectivo(1L)
                .nombre("2026-2027")
                .fechaInicio(LocalDate.of(2026, 5, 2))
                .fechaFin(LocalDate.of(2027, 2, 28))
                .esActual(true)
                .creadoPor(usuario)
                .build();

        requestDTO = new AnoLectivoRequestDTO();
        requestDTO.setNombre("2026-2027");
        requestDTO.setFechaInicio(LocalDate.of(2026, 5, 2));
        requestDTO.setFechaFin(LocalDate.of(2027, 2, 28));
    }

    // ─── 1. LISTADO Y CONSULTA ──────────────────────────────────────────────
    @Test
    @DisplayName("1. Listar todos los años lectivos — Retorna lista de DTOs")
    void listarTodos_retornaLista() {
        given(anoLectivoRepo.findAll()).willReturn(List.of(anoLectivo));

        List<AnoLectivoResponseDTO> lista = anoLectivoService.listarTodos();

        assertThat(lista).isNotEmpty();
        assertThat(lista.size()).isEqualTo(1);
        assertThat(lista.get(0).getNombre()).isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("2. Obtener por ID — Existe — Retorna DTO")
    void obtenerPorId_whenExiste_retornaDTO() {
        given(anoLectivoRepo.findById(1L)).willReturn(Optional.of(anoLectivo));

        AnoLectivoResponseDTO dto = anoLectivoService.obtenerPorId(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getIdAnoLectivo()).isEqualTo(1L);
        assertThat(dto.getNombre()).isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("3. Obtener por ID — No existe — Lanza 404 NOT_FOUND")
    void obtenerPorId_whenNoExiste_throwsNotFound() {
        given(anoLectivoRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> anoLectivoService.obtenerPorId(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── 2. CREACIÓN ────────────────────────────────────────────────────────
    @Test
    @DisplayName("4. Crear año lectivo — Datos válidos — Guarda y retorna DTO")
    void crear_whenValido_guardaYRetornaDTO() {
        given(anoLectivoRepo.existsByNombre("2026-2027")).willReturn(false);
        given(usuarioRepo.findByUsername("admin")).willReturn(Optional.of(usuario));
        given(anoLectivoRepo.save(any(AnoLectivo.class))).willReturn(anoLectivo);

        AnoLectivoResponseDTO result = anoLectivoService.crear(requestDTO, "admin");

        assertThat(result).isNotNull();
        assertThat(result.getNombre()).isEqualTo("2026-2027");
        then(anoLectivoRepo).should(times(1)).save(any(AnoLectivo.class));
    }

    @Test
    @DisplayName("5. Crear año lectivo — Nombre duplicado — Lanza 409 CONFLICT")
    void crear_whenNombreDuplicado_throwsConflict() {
        given(anoLectivoRepo.existsByNombre("2026-2027")).willReturn(true);

        assertThatThrownBy(() -> anoLectivoService.crear(requestDTO, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(anoLectivoRepo).should(never()).save(any());
    }

    @Test
    @DisplayName("6. Crear año lectivo — Fecha fin anterior a inicio — Lanza 400 BAD_REQUEST")
    void crear_whenFechasInvalidas_throwsBadRequest() {
        requestDTO.setFechaFin(LocalDate.of(2025, 1, 1));
        given(anoLectivoRepo.existsByNombre("2026-2027")).willReturn(false);

        assertThatThrownBy(() -> anoLectivoService.crear(requestDTO, "admin"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    // ─── 3. ACTUALIZACIÓN Y ACTIVACIÓN ──────────────────────────────────────
    @Test
    @DisplayName("7. Actualizar año lectivo — Datos válidos — Guarda cambios")
    void actualizar_whenValido_actualiza() {
        given(anoLectivoRepo.findById(1L)).willReturn(Optional.of(anoLectivo));
        given(anoLectivoRepo.save(any(AnoLectivo.class))).willReturn(anoLectivo);

        requestDTO.setNombre("2026-2027 (Modificado)");
        AnoLectivoResponseDTO result = anoLectivoService.actualizar(1L, requestDTO);

        assertThat(result).isNotNull();
        then(anoLectivoRepo).should(times(1)).save(anoLectivo);
    }

    @Test
    @DisplayName("8. Establecer como año actual — Desactiva anteriores y activa seleccionado")
    void establecerActual_desactivaYActiva() {
        AnoLectivo ano2 = AnoLectivo.builder()
                .idAnoLectivo(2L)
                .nombre("2025-2026")
                .esActual(true)
                .build();

        given(anoLectivoRepo.findAll()).willReturn(List.of(ano2));
        given(anoLectivoRepo.findById(1L)).willReturn(Optional.of(anoLectivo));

        anoLectivoService.establecerActual(1L);

        assertThat(ano2.isEsActual()).isFalse();
        assertThat(anoLectivo.isEsActual()).isTrue();
        then(anoLectivoRepo).should(times(2)).save(any(AnoLectivo.class));
    }

    @Test
    @DisplayName("9. Obtener año actual — Existe — Retorna DTO")
    void obtenerActual_whenExiste_retornaDTO() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));

        AnoLectivoResponseDTO actual = anoLectivoService.obtenerActual();

        assertThat(actual).isNotNull();
        assertThat(actual.isEsActual()).isTrue();
    }

    @Test
    @DisplayName("10. Obtener año actual — No existe — Lanza 404 NOT_FOUND")
    void obtenerActual_whenNoExiste_throwsNotFound() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.empty());

        assertThatThrownBy(() -> anoLectivoService.obtenerActual())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }
}
