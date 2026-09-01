package ec.edu.uteq.sga.domain.strategy;

import java.math.BigDecimal;
import java.util.List;

/**
 * Patrón GoF Strategy: Define la interfaz para diferentes algoritmos
 * de cálculo de promedios y calificaciones en el SGA.
 */
public interface CalculoPromedioStrategy {
    BigDecimal calcularPromedio(List<BigDecimal> calificaciones);
    String getIdentificador();
}