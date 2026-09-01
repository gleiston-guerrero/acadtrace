package ec.edu.uteq.sga.application.report;

import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/**
 * Implementación concreta del Template Method para el Acta de Calificaciones por Período.
 */
@Component
public class ReporteNotasPeriodoPDF extends GeneradorReporteAcademicoTemplate {

    @Override
    @SuppressWarnings("unchecked")
    protected String procesarCuerpo(Map<String, Object> parametros) {
        StringBuilder sb = new StringBuilder();
        sb.append("CURSO: ").append(parametros.getOrDefault("curso", "General")).append("\n");
        sb.append("PERÍODO: ").append(parametros.getOrDefault("periodo", "2026-2027")).append("\n");
        sb.append("--------------------------------------------------------\n");
        sb.append(String.format("%-15s %-25s %-10s\n", "CÉDULA", "ESTUDIANTE", "PROMEDIO"));
        sb.append("--------------------------------------------------------\n");

        List<Map<String, Object>> filas = (List<Map<String, Object>>) parametros.get("filas");
        if (filas != null) {
            for (Map<String, Object> fila : filas) {
                sb.append(String.format("%-15s %-25s %-10s\n",
                        fila.get("cedula"),
                        fila.get("nombre"),
                        fila.get("promedio")));
            }
        } else {
            sb.append("Sin registros de calificaciones para el período seleccionado.\n");
        }

        return sb.toString();
    }
}