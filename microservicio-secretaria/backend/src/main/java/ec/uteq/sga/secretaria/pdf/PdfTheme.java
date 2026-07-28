package ec.uteq.sga.secretaria.pdf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.awt.Color;

/** Colores y datos institucionales, equivalentes a COLOR/INST en pdfGenerator.js. */
@Component
public class PdfTheme {

    public static final Color PRIMARY = Color.decode("#1a3a5c");
    public static final Color SECONDARY = Color.decode("#2e86ab");
    public static final Color ACCENT = Color.decode("#f5a623");
    public static final Color LIGHT = Color.decode("#f4f6f8");
    public static final Color DARK = Color.decode("#2c3e50");
    public static final Color TEXT = Color.decode("#333333");
    public static final Color MUTED = Color.decode("#6c757d");
    public static final Color WHITE = Color.WHITE;
    public static final Color BORDER = Color.decode("#dee2e6");
    public static final Color HEADER_SUBTITLE = Color.decode("#b8d4f0");
    public static final Color HEADER_SUBSUBTITLE = Color.decode("#90b8d8");

    private final String nombre;
    private final String ciudad;

    public PdfTheme(@Value("${app.institucion.nombre}") String nombre,
                     @Value("${app.institucion.ciudad}") String ciudad) {
        this.nombre = nombre;
        this.ciudad = ciudad;
    }

    public String nombre() {
        return nombre;
    }

    public String ciudad() {
        return ciudad;
    }
}
