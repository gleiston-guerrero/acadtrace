package ec.uteq.sga.secretaria.domain.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

public record ActualizarHorasGradoRequest(
        @NotNull(message = "El grado es obligatorio") Long idGrado,
        @NotNull(message = "El año lectivo es obligatorio") Long idAnoLectivo,
        List<Map<String, Object>> cambios
) {}
