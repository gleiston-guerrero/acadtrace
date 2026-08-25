package ec.uteq.sga.secretaria.domain.dto;

import java.util.Map;

public record IaDiagnosticoRequest(
    Long idMatricula,
    String estudiante,
    String grado,
    String paralelo,
    String materia,
    Integer trimestre,
    Double promedio,
    Double porcentajeAsistencia,
    Map<String, Object> detalleNotas
) {}