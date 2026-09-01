package ec.uteq.sga.secretaria.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EventoAcademicoRequest(
        @NotBlank(message = "Título requerido") String titulo,
        String descripcion,
        @NotNull(message = "Fecha de inicio requerida") LocalDate fecha_inicio,
        LocalDate fecha_fin,
        String tipo,
        Long id_grado
) {}
