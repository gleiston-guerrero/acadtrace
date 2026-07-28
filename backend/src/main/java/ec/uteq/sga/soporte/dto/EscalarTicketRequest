package ec.uteq.sga.soporte.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Escalamiento de un ticket: sube la prioridad y/o lo reasigna a un tecnico
 * superior, dejando registro del motivo. nuevaPrioridad y nuevoAsignado son
 * opcionales (al menos uno debe venir), motivo siempre es obligatorio para
 * que quede trazabilidad de por que se escalo.
 */
public record EscalarTicketRequest(
        String nuevaPrioridad,

        String nuevoAsignado,

        @NotBlank(message = "El motivo del escalamiento es obligatorio")
        String motivo
) {}
