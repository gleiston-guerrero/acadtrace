package ec.uteq.sga.soporte.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Actualizacion combinada de un ticket desde el detalle: estado + tecnico
 * asignado + solucion aplicada. asignadoA y solucionAplicada son opcionales.
 */
public record ActualizarTicketRequest(
        @NotBlank(message = "El estado es obligatorio")
        String estado,

        String asignadoA,

        String solucionAplicada
) {}
