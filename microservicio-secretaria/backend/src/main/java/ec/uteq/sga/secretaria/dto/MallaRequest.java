package ec.uteq.sga.secretaria.dto;

import jakarta.validation.constraints.NotNull;

public record MallaRequest(
        @NotNull(message = "El grado es obligatorio") Long idGrado,
        @NotNull(message = "La asignatura es obligatoria") Long idAsignatura,
        @NotNull(message = "El año lectivo es obligatorio") Long idAnoLectivo,
        @NotNull(message = "Las horas semanales son obligatorias") Short horasSemana,
        Short diasSemana,
        Short duracion
) {}
