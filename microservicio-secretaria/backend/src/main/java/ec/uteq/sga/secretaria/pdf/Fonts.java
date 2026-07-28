package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

/** Fuentes estandar (no embebidas), equivalentes a 'Helvetica'/'Helvetica-Bold' en pdfkit. */
public final class Fonts {

    public static final PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    public static final PDFont HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private Fonts() {}
}
