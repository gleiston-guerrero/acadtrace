package ec.uteq.sga.secretaria.domain.dto;

public record IaCitacionRequest(
    Long idMatricula,
    String estudiante,
    String representante,
    String motivo,
    String fechaCitacion,
    String horaCitacion
) {}