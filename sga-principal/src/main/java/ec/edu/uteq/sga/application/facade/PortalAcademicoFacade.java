package ec.edu.uteq.sga.application.facade;

import ec.edu.uteq.sga.application.event.NotaPublicadaEvent;
import ec.edu.uteq.sga.application.service.EstudianteService;
import ec.edu.uteq.sga.domain.dto.EstudianteDetalleDTO;
import ec.edu.uteq.sga.domain.strategy.CalculoPromedioStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Patrón GoF Facade: Fachada unificada que simplifica las operaciones del portal académico
 * coordinando EstudianteService, cálculo de estrategias de promedios y despacho de eventos.
 */
@Service
public class PortalAcademicoFacade {

    private static final Logger log = LoggerFactory.getLogger(PortalAcademicoFacade.class);

    private final EstudianteService estudianteService;
    private final CalculoPromedioStrategy estrategiaPromedio;
    private final ApplicationEventPublisher eventPublisher;

    public PortalAcademicoFacade(EstudianteService estudianteService,
                                 @Qualifier("promedio7030Strategy") CalculoPromedioStrategy estrategiaPromedio,
                                 ApplicationEventPublisher eventPublisher) {
        this.estudianteService = estudianteService;
        this.estrategiaPromedio = estrategiaPromedio;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Operación de Fachada: Obtiene el expediente consolidado del estudiante con su promedio ponderado.
     */
    public Map<String, Object> obtenerExpedienteConsolidado(Long idEstudiante, List<BigDecimal> notasParciales) {
        log.info("[FACADE] Coordinando consulta de expediente consolidado para ID {}", idEstudiante);

        EstudianteDetalleDTO estudiante = estudianteService.obtener(idEstudiante);
        BigDecimal promedioCalculado = estrategiaPromedio.calcularPromedio(notasParciales);

        Map<String, Object> expediente = new HashMap<>();
        expediente.put("estudiante", estudiante);
        expediente.put("promedioPonderado", promedioCalculado);
        expediente.put("reglaCalculo", estrategiaPromedio.getIdentificador());
        expediente.put("estadoAcademico", promedioCalculado.compareTo(BigDecimal.valueOf(7.0)) >= 0 ? "APROBADO" : "EN_RECUPERACION");

        return expediente;
    }

    /**
     * Operación de Fachada: Registra una calificación y dispara el evento del observador.
     */
    public void registrarCalificacionYNotificar(Long idEstudiante, String materia, BigDecimal nota, String docente) {
        EstudianteDetalleDTO estudiante = estudianteService.obtener(idEstudiante);

        log.info("[FACADE] Registrando nota {} en {} para alumno {}", nota, materia, estudiante.getCedula());

        // Disparo del patrón Observer
        eventPublisher.publishEvent(new NotaPublicadaEvent(
                idEstudiante,
                estudiante.getCedula(),
                materia,
                nota,
                docente
        ));
    }
}