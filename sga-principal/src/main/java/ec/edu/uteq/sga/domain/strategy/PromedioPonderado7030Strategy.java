package ec.edu.uteq.sga.domain.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Estrategia oficial: 70% Evaluaciones Formativas / 30% Examen Quimestral.
 */
@Component("promedio7030Strategy")
public class PromedioPonderado7030Strategy implements CalculoPromedioStrategy {

    @Override
    public BigDecimal calcularPromedio(List<BigDecimal> calificaciones) {
        if (calificaciones == null || calificaciones.isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Si hay al menos 2 notas: la última es examen (30%) y las anteriores son actividades (70%)
        if (calificaciones.size() == 1) {
            return calificaciones.get(0).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sumaActividades = BigDecimal.ZERO;
        int totalActividades = calificaciones.size() - 1;

        for (int i = 0; i < totalActividades; i++) {
            sumaActividades = sumaActividades.add(calificaciones.get(i));
        }

        BigDecimal promedioActividades = sumaActividades.divide(BigDecimal.valueOf(totalActividades), 4, RoundingMode.HALF_UP);
        BigDecimal notaExamen = calificaciones.get(calificaciones.size() - 1);

        // Promedio = (Actividades * 0.70) + (Examen * 0.30)
        BigDecimal componenteFormativo = promedioActividades.multiply(BigDecimal.valueOf(0.70));
        BigDecimal componenteExamen = notaExamen.multiply(BigDecimal.valueOf(0.30));

        return componenteFormativo.add(componenteExamen).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public String getIdentificador() {
        return "PONDERADO_70_30";
    }
}