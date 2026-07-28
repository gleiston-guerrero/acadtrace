package ec.uteq.sga.secretaria.dto;

import java.util.List;

public record ConfirmarImportacionRequest(
        List<ImportacionEstudianteRow> estudiantes,
        Long id_grado,
        Long id_paralelo,
        Long id_ano_lectivo
) {}
