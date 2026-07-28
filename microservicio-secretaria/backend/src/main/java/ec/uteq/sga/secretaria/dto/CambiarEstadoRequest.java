package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO compartido por los modulos cuyo "estado" es booleano (estudiantes, usuarios).
 * Matriculas usa un estado enumerado en string, con su propio DTO.
 */
public record CambiarEstadoRequest(@NotNull(message = "estado requerido") Boolean estado) {}
