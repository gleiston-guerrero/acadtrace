package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.configuracion.EscalaCalificacionDTO;
import ec.edu.uteq.sga.domain.dto.configuracion.EsquemaCalificacionDTO;
import ec.edu.uteq.sga.domain.dto.configuracion.PeriodoEvaluacionDTO;
import ec.edu.uteq.sga.domain.dto.configuracion.TipoAporteDTO;
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

import java.math.BigDecimal;
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
@DisplayName("Pruebas Unitarias: ConfiguracionCalificacionService (SGA Principal)")
class ConfiguracionCalificacionServiceTest {

    @Mock
    private EsquemaCalificacionRepository esquemaRepo;

    @Mock
    private TipoAporteRepository tipoAporteRepo;

    @Mock
    private EscalaCalificacionesRepository escalaRepo;

    @Mock
    private PeriodoEvaluacionRepository periodoRepo;

    @Mock
    private AnoLectivoRepository anoLectivoRepo;

    @Mock
    private NivelEducativoRepository nivelRepo;

    @InjectMocks
    private ConfiguracionCalificacionService configuracionService;

    private AnoLectivo anoLectivo;
    private EsquemaCalificacion esquema;
    private NivelEducativo nivel;
    private EscalaCalificaciones escala;
    private TipoAporte tipoAporte;
    private PeriodoEvaluacion periodo;

    @BeforeEach
    void setUp() {
        anoLectivo = AnoLectivo.builder()
                .idAnoLectivo(1L)
                .nombre("2026-2027")
                .esActual(true)
                .build();

        esquema = EsquemaCalificacion.builder()
                .idEsquema(10L)
                .anoLectivo(anoLectivo)
                .pesoFormativa(new BigDecimal("70.00"))
                .pesoSumativa(new BigDecimal("30.00"))
                .build();

        nivel = NivelEducativo.builder()
                .idNivel(2L)
                .nombre("Básica Elemental")
                .build();

        escala = EscalaCalificaciones.builder()
                .idEscala(100L)
                .anoLectivo(anoLectivo)
                .nivel(nivel)
                .notaMinima(new BigDecimal("9.00"))
                .notaMaxima(new BigDecimal("10.00"))
                .equivalenteCualitativo("DAR")
                .descripcion("Domina los aprendizajes requeridos")
                .build();

        tipoAporte = TipoAporte.builder()
                .idTipoAporte(5L)
                .anoLectivo(anoLectivo)
                .nombre("Lecciones Escritas")
                .tipoEvaluacion("FORMATIVA")
                .orden(1)
                .activo(true)
                .build();

        periodo = PeriodoEvaluacion.builder()
                .idPeriodo(1L)
                .anoLectivo(anoLectivo)
                .tipo("TRIMESTRE")
                .nombre("Primer Trimestre")
                .fechaInicio(LocalDate.of(2026, 5, 2))
                .fechaFin(LocalDate.of(2026, 8, 15))
                .activo(true)
                .build();
    }

    // ─── 1. ESQUEMA DE CALIFICACIÓN ─────────────────────────────────────────
    @Test
    @DisplayName("1. Obtener esquema existente — Retorna DTO con 70/30")
    void obtenerEsquema_whenExiste_retornaDTO() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));
        given(esquemaRepo.findByAnoLectivo_IdAnoLectivo(1L)).willReturn(Optional.of(esquema));

        EsquemaCalificacionDTO dto = configuracionService.obtenerEsquema();

        assertThat(dto).isNotNull();
        assertThat(dto.getPesoFormativa()).isEqualByComparingTo("70.00");
        assertThat(dto.getPesoSumativa()).isEqualByComparingTo("30.00");
    }

    @Test
    @DisplayName("2. Guardar esquema — Pesos suman 100 — Guarda exitosamente")
    void guardarEsquema_whenSuman100_guarda() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));
        given(esquemaRepo.findByAnoLectivo_IdAnoLectivo(1L)).willReturn(Optional.of(esquema));
        given(esquemaRepo.save(any(EsquemaCalificacion.class))).willReturn(esquema);

        EsquemaCalificacionDTO nuevoEsquema = EsquemaCalificacionDTO.builder()
                .pesoFormativa(new BigDecimal("60.00"))
                .pesoSumativa(new BigDecimal("40.00"))
                .build();

        EsquemaCalificacionDTO result = configuracionService.guardarEsquema(nuevoEsquema);

        assertThat(result).isNotNull();
        then(esquemaRepo).should(times(1)).save(any(EsquemaCalificacion.class));
    }

    @Test
    @DisplayName("3. Guardar esquema — Pesos no suman 100 — Lanza 400 BAD_REQUEST")
    void guardarEsquema_whenNoSuman100_throwsBadRequest() {
        EsquemaCalificacionDTO invalido = EsquemaCalificacionDTO.builder()
                .pesoFormativa(new BigDecimal("50.00"))
                .pesoSumativa(new BigDecimal("40.00"))
                .build();

        assertThatThrownBy(() -> configuracionService.guardarEsquema(invalido))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        then(esquemaRepo).should(never()).save(any());
    }

    // ─── 2. TIPOS DE APORTE ─────────────────────────────────────────────────
    @Test
    @DisplayName("4. Crear tipo de aporte — Válido — Guarda y retorna DTO")
    void crearTipoAporte_whenValido_guarda() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));
        given(tipoAporteRepo.existsByAnoLectivo_IdAnoLectivoAndNombreIgnoreCase(1L, "Deberes")).willReturn(false);
        given(tipoAporteRepo.save(any(TipoAporte.class))).willReturn(tipoAporte);

        TipoAporteDTO dto = TipoAporteDTO.builder()
                .nombre("Deberes")
                .tipoEvaluacion("FORMATIVA")
                .orden(1)
                .build();

        TipoAporteDTO result = configuracionService.crearTipoAporte(dto);

        assertThat(result).isNotNull();
        then(tipoAporteRepo).should(times(1)).save(any(TipoAporte.class));
    }

    @Test
    @DisplayName("5. Crear tipo de aporte — Tipo inválido — Lanza 400 BAD_REQUEST")
    void crearTipoAporte_whenTipoInvalido_throwsBadRequest() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));

        TipoAporteDTO dto = TipoAporteDTO.builder()
                .nombre("Deberes")
                .tipoEvaluacion("OTRO_TIPO")
                .build();

        assertThatThrownBy(() -> configuracionService.crearTipoAporte(dto))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
    }

    // ─── 3. ESCALA CUALITATIVA ──────────────────────────────────────────────
    @Test
    @DisplayName("6. Convertir nota cuantitativa a cualitativa (DAR)")
    void convertirACualitativa_whenNotaEnRango_retornaEscala() {
        given(anoLectivoRepo.findByEsActualTrue()).willReturn(Optional.of(anoLectivo));
        given(escalaRepo.findByAnoLectivo_IdAnoLectivo(1L)).willReturn(List.of(escala));

        EscalaCalificacionDTO resultado = configuracionService.convertirACualitativa(2L, new BigDecimal("9.50"));

        assertThat(resultado).isNotNull();
        assertThat(resultado.getEquivalenteCualitativo()).isEqualTo("DAR");
    }

    // ─── 4. PERIODOS DE EVALUACIÓN ──────────────────────────────────────────
    @Test
    @DisplayName("7. Actualizar periodo de evaluación — Fechas válidas")
    void actualizarPeriodo_whenValido_actualiza() {
        given(periodoRepo.findById(1L)).willReturn(Optional.of(periodo));
        given(periodoRepo.save(any(PeriodoEvaluacion.class))).willReturn(periodo);

        PeriodoEvaluacionDTO dto = PeriodoEvaluacionDTO.builder()
                .nombre("Primer Trimestre Modificado")
                .fechaInicio(LocalDate.of(2026, 5, 2))
                .fechaFin(LocalDate.of(2026, 8, 20))
                .activo(true)
                .build();

        PeriodoEvaluacionDTO result = configuracionService.actualizarPeriodo(1L, dto);

        assertThat(result).isNotNull();
        then(periodoRepo).should(times(1)).save(periodo);
    }
}
