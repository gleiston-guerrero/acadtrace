package ec.edu.uteq.sga.domain.strategy;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Estrategia alternativa: Promedio aritmético simple (suma de notas dividida para N).
 */
@Component("promedioSimpleStrategy")
public class PromedioAritmeticoSimpleStrategy implements CalculoPromedioStrategy {

    @Override
    public BigDecimal calcularPromedio(List<BigDecimal> calificaciones) {
        if (calificaciones == null || calificaciones.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal suma = BigDecimal.ZERO;
        for (BigDecimal nota : calificaciones) {
            suma = suma.add(nota);
        }

        return suma.divide(BigDecimal.valueOf(calificaciones.size()), 2, RoundingMode.HALF_UP);
    }

    @Override
    public String getIdentificador() {
        return "ARITMETICO_SIMPLE";
    }
}