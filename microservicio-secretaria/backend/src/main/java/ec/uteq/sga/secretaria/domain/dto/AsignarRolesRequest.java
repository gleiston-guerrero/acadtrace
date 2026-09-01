package ec.uteq.sga.secretaria.domain.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AsignarRolesRequest(@NotNull(message = "roles requerido") List<String> roles) {}
