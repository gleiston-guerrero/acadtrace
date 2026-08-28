package ec.uteq.sga.secretaria.infrastructure.common;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Feriados nacionales de Ecuador (fijos + móviles calculados desde Pascua). No hay tabla para esto en ningún repo del sistema. */
public final class FeriadosEcuador {

    private FeriadosEcuador() {}

    public static List<Map<String, Object>> delAno(int anio) {
        LocalDate pascua = domingoPascua(anio);

        return List.of(
                evento("Año Nuevo", LocalDate.of(anio, 1, 1)),
                evento("Carnaval", pascua.minusDays(48)),
                evento("Carnaval", pascua.minusDays(47)),
                evento("Viernes Santo", pascua.minusDays(2)),
                evento("Día del Trabajo", LocalDate.of(anio, 5, 1)),
                evento("Batalla de Pichincha", LocalDate.of(anio, 5, 24)),
                evento("Primer Grito de la Independencia", LocalDate.of(anio, 8, 10)),
                evento("Independencia de Guayaquil", LocalDate.of(anio, 10, 9)),
                evento("Día de los Difuntos", LocalDate.of(anio, 11, 2)),
                evento("Independencia de Cuenca", LocalDate.of(anio, 11, 3)),
                evento("Navidad", LocalDate.of(anio, 12, 25))
        );
    }

    private static Map<String, Object> evento(String titulo, LocalDate fecha) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("titulo", titulo);
        row.put("fecha_inicio", fecha);
        row.put("fecha_fin", null);
        row.put("tipo", "FERIADO");
        return row;
    }

    /** Algoritmo de Meeus/Jones/Butcher (calendario gregoriano). */
    private static LocalDate domingoPascua(int anio) {
        int a = anio % 19;
        int b = anio / 100;
        int c = anio % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;
        return LocalDate.of(anio, mes, dia);
    }
}
