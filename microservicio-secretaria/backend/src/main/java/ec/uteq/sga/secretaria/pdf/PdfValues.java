package ec.uteq.sga.secretaria.pdf;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Conversion de valores crudos del RowMapper (String/Number/LocalDate/null) a texto para el PDF. */
public final class PdfValues {

    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("d/M/yyyy");

    private PdfValues() {}

    public static String str(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    public static String formatFechaEc(Object value) {
        LocalDate date = switch (value) {
            case null -> null;
            case LocalDate d -> d;
            case LocalDateTime dt -> dt.toLocalDate();
            default -> null;
        };
        return date == null ? "" : date.format(SHORT_DATE);
    }
}
