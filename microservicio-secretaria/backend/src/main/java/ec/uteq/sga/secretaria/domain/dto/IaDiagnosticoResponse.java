package ec.uteq.sga.secretaria.domain.dto;

import java.util.List;

public record IaDiagnosticoResponse(
    Long idMatricula,
    String estudiante,
    String materia,
    Double promedio,
    String escalaCualitativa,
    String nivelRiesgo,
    List<String> fortalezas,
    List<String> areasDeMejora,
    String recomendacionPedagogica,
    boolean alertaRepresentante,
    String citacionSugerida,
    String motorIa,
    String fechaAnalisis
) {}