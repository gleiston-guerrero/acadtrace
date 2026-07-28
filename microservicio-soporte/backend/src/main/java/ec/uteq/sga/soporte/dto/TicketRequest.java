package ec.uteq.sga.soporte.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos para crear un ticket de soporte. El estado inicial siempre es ABIERTO,
 * el numero de ticket se genera en el servidor y el creador se toma del JWT.
 */
public record TicketRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "La descripción es obligatoria")
        String descripcion,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        @NotBlank(message = "La prioridad es obligatoria")
        String prioridad
) {}
