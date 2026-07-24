package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.configuracion.*;
import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Configuración de CÓMO se califica: trimestres, ponderación formativa/sumativa,
 * tipos de aporte y escala cualitativa.
 *
 * Importante: aquí sólo vive la CONFIGURACIÓN. Las notas de los estudiantes las
 * registra y almacena el microservicio docente en su propio esquema (sga_docente);
 * ese microservicio consulta esta configuración para calcular los promedios.
 */
@Service
@RequiredArgsConstructor
public class ConfiguracionCalificacionService {

    private static final Set<String> TIPOS_EVALUACION = Set.of("FORMATIVA", "SUMATIVA");

    private final EsquemaCalificacionRepository esquemaRepo;
    private final TipoAporteRepository tipoAporteRepo;
    private final EscalaCalificacionesRepository escalaRepo;
    private final PeriodoEvaluacionRepository periodoRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final NivelEducativoRepository nivelRepo;

    private AnoLectivo anoActual() {
        return anoLectivoRepo.findByEsActualTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "No hay un año lectivo activo configurado"));
    }

    // ---------------------------------------------------------------- esquema

    @Transactional(readOnly = true)
    public EsquemaCalificacionDTO obtenerEsquema() {
        AnoLectivo ano = anoActual();
        return esquemaRepo.findByAnoLectivo_IdAnoLectivo(ano.getIdAnoLectivo())
                .map(e -> EsquemaCalificacionDTO.builder()
                        .idEsquema(e.getIdEsquema())
                        .pesoFormativa(e.getPesoFormativa())
                        .pesoSumativa(e.getPesoSumativa())
                        .build())
                // Si aún no se ha configurado, se devuelve el estándar 70/30.
                .orElseGet(() -> EsquemaCalificacionDTO.builder()
                        .pesoFormativa(new BigDecimal("70.00"))
                        .pesoSumativa(new BigDecimal("30.00"))
                        .build());
    }

    @Transactional
    public EsquemaCalificacionDTO guardarEsquema(EsquemaCalificacionDTO dto) {
        BigDecimal suma = dto.getPesoFormativa().add(dto.getPesoSumativa());
        if (suma.compareTo(new BigDecimal("100")) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Los pesos deben sumar 100 (actual: " + suma + ")");
        }

        AnoLectivo ano = anoActual();
        EsquemaCalificacion esquema = esquemaRepo.findByAnoLectivo_IdAnoLectivo(ano.getIdAnoLectivo())
                .orElseGet(() -> EsquemaCalificacion.builder().anoLectivo(ano).build());
        esquema.setPesoFormativa(dto.getPesoFormativa());
        esquema.setPesoSumativa(dto.getPesoSumativa());
        esquemaRepo.save(esquema);

        return obtenerEsquema();
    }

    // ---------------------------------------------------------- tipos aporte

    @Transactional(readOnly = true)
    public List<TipoAporteDTO> listarTiposAporte() {
        AnoLectivo ano = anoActual();
        return tipoAporteRepo.findByAnoLectivo_IdAnoLectivoOrderByTipoEvaluacionAscOrdenAsc(ano.getIdAnoLectivo())
                .stream().map(this::aDto).toList();
    }

    @Transactional
    public TipoAporteDTO crearTipoAporte(TipoAporteDTO dto) {
        AnoLectivo ano = anoActual();
        validarTipoEvaluacion(dto.getTipoEvaluacion());

        if (tipoAporteRepo.existsByAnoLectivo_IdAnoLectivoAndNombreIgnoreCase(
                ano.getIdAnoLectivo(), dto.getNombre())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un aporte con ese nombre en el año lectivo");
        }

        TipoAporte aporte = TipoAporte.builder()
                .anoLectivo(ano)
                .nombre(dto.getNombre())
                .tipoEvaluacion(dto.getTipoEvaluacion().toUpperCase())
                .orden(dto.getOrden() == null ? 0 : dto.getOrden())
                .activo(dto.getActivo() == null || dto.getActivo())
                .build();
        return aDto(tipoAporteRepo.save(aporte));
    }

    @Transactional
    public TipoAporteDTO actualizarTipoAporte(Long id, TipoAporteDTO dto) {
        TipoAporte aporte = tipoAporteRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de aporte no encontrado"));
        validarTipoEvaluacion(dto.getTipoEvaluacion());

        aporte.setNombre(dto.getNombre());
        aporte.setTipoEvaluacion(dto.getTipoEvaluacion().toUpperCase());
        if (dto.getOrden() != null) aporte.setOrden(dto.getOrden());
        if (dto.getActivo() != null) aporte.setActivo(dto.getActivo());
        return aDto(tipoAporteRepo.save(aporte));
    }

    @Transactional
    public void eliminarTipoAporte(Long id) {
        if (!tipoAporteRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de aporte no encontrado");
        }
        tipoAporteRepo.deleteById(id);
    }

    // ----------------------------------------------------------------- escala

    @Transactional(readOnly = true)
    public List<EscalaCalificacionDTO> listarEscala(Long idNivel) {
        AnoLectivo ano = anoActual();
        return escalaRepo.findByAnoLectivo_IdAnoLectivo(ano.getIdAnoLectivo()).stream()
                .filter(e -> idNivel == null || e.getNivel().getIdNivel().equals(idNivel))
                .sorted((a, b) -> b.getNotaMinima().compareTo(a.getNotaMinima()))
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public EscalaCalificacionDTO crearRangoEscala(EscalaCalificacionDTO dto) {
        AnoLectivo ano = anoActual();
        validarRango(dto);
        NivelEducativo nivel = nivelRepo.findById(dto.getIdNivel())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nivel educativo no encontrado"));

        EscalaCalificaciones escala = EscalaCalificaciones.builder()
                .anoLectivo(ano)
                .nivel(nivel)
                .notaMinima(dto.getNotaMinima())
                .notaMaxima(dto.getNotaMaxima())
                .equivalenteCualitativo(dto.getEquivalenteCualitativo())
                .descripcion(dto.getDescripcion())
                .build();
        return aDto(escalaRepo.save(escala));
    }

    @Transactional
    public EscalaCalificacionDTO actualizarRangoEscala(Long id, EscalaCalificacionDTO dto) {
        EscalaCalificaciones escala = escalaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rango de escala no encontrado"));
        validarRango(dto);

        escala.setNotaMinima(dto.getNotaMinima());
        escala.setNotaMaxima(dto.getNotaMaxima());
        escala.setEquivalenteCualitativo(dto.getEquivalenteCualitativo());
        escala.setDescripcion(dto.getDescripcion());
        return aDto(escalaRepo.save(escala));
    }

    @Transactional
    public void eliminarRangoEscala(Long id) {
        if (!escalaRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rango de escala no encontrado");
        }
        escalaRepo.deleteById(id);
    }

    /**
     * Convierte una nota cuantitativa en su equivalente cualitativo según la
     * escala del nivel. Es la misma conversión que consumirá el docente.
     */
    @Transactional(readOnly = true)
    public EscalaCalificacionDTO convertirACualitativa(Long idNivel, BigDecimal nota) {
        return listarEscala(idNivel).stream()
                .filter(e -> nota.compareTo(e.getNotaMinima()) >= 0 && nota.compareTo(e.getNotaMaxima()) <= 0)
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "La nota " + nota + " no cae en ningún rango de la escala"));
    }

    // -------------------------------------------------------------- periodos

    @Transactional(readOnly = true)
    public List<PeriodoEvaluacionDTO> listarPeriodos() {
        AnoLectivo ano = anoActual();
        return periodoRepo.findByAnoLectivo_IdAnoLectivo(ano.getIdAnoLectivo()).stream()
                .sorted((a, b) -> a.getFechaInicio().compareTo(b.getFechaInicio()))
                .map(this::aDto)
                .toList();
    }

    @Transactional
    public PeriodoEvaluacionDTO actualizarPeriodo(Long id, PeriodoEvaluacionDTO dto) {
        PeriodoEvaluacion periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Periodo no encontrado"));
        if (dto.getFechaFin().isBefore(dto.getFechaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la de inicio");
        }
        periodo.setNombre(dto.getNombre());
        periodo.setFechaInicio(dto.getFechaInicio());
        periodo.setFechaFin(dto.getFechaFin());
        if (dto.getActivo() != null) periodo.setActivo(dto.getActivo());
        return aDto(periodoRepo.save(periodo));
    }

    // -------------------------------------------------------------- helpers

    private void validarTipoEvaluacion(String tipo) {
        if (tipo == null || !TIPOS_EVALUACION.contains(tipo.toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tipo de evaluación debe ser FORMATIVA o SUMATIVA");
        }
    }

    private void validarRango(EscalaCalificacionDTO dto) {
        if (dto.getNotaMinima().compareTo(dto.getNotaMaxima()) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La nota mínima no puede ser mayor que la máxima");
        }
    }

    private TipoAporteDTO aDto(TipoAporte a) {
        return TipoAporteDTO.builder()
                .idTipoAporte(a.getIdTipoAporte())
                .nombre(a.getNombre())
                .tipoEvaluacion(a.getTipoEvaluacion())
                .orden(a.getOrden())
                .activo(a.isActivo())
                .build();
    }

    private EscalaCalificacionDTO aDto(EscalaCalificaciones e) {
        return EscalaCalificacionDTO.builder()
                .idEscala(e.getIdEscala())
                .idNivel(e.getNivel().getIdNivel())
                .nivel(e.getNivel().getNombre())
                .notaMinima(e.getNotaMinima())
                .notaMaxima(e.getNotaMaxima())
                .equivalenteCualitativo(e.getEquivalenteCualitativo())
                .descripcion(e.getDescripcion())
                .build();
    }

    private PeriodoEvaluacionDTO aDto(PeriodoEvaluacion p) {
        return PeriodoEvaluacionDTO.builder()
                .idPeriodo(p.getIdPeriodo())
                .tipo(p.getTipo())
                .nombre(p.getNombre())
                .fechaInicio(p.getFechaInicio())
                .fechaFin(p.getFechaFin())
                .activo(p.isActivo())
                .build();
    }
}
