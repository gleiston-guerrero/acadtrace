package ec.uteq.sga.soporte.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Agrega un comentario / seguimiento a un ticket. notaInterna marca si es
 * visible solo para el equipo de soporte (opcional, por defecto false).
 */
public record ComentarioRequest(
        @NotBlank(message = "El contenido es obligatorio")
        String contenido,

        Boolean notaInterna
) {}
