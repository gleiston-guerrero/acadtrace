package ec.uteq.sga.secretaria.dto;

import java.util.List;

public record ImportacionResultado(
        List<ImportacionEstudianteRow> estudiantes,
        int totalFilas,
        int filasValidas,
        int filasConError
) {}
