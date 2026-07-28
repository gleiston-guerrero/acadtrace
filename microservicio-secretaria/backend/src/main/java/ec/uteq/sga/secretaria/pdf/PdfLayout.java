package ec.uteq.sga.secretaria.pdf;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Cabecera/pie/secciones/campos comunes a los 3 reportes PDF (portados de pdfGenerator.js). */
public final class PdfLayout {

    private static final DateTimeFormatter FOOTER_DATE_FORMAT =
            DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", new Locale("es", "EC"));

    private PdfLayout() {}

    public static float drawHeader(PdfCanvas canvas, PdfTheme theme, String titulo, String subtitulo) throws IOException {
        float pageWidth = canvas.pageWidth();

        canvas.rect(0, 0, pageWidth, 90, PdfTheme.PRIMARY);
        canvas.textAligned(40, 22, pageWidth - 80, theme.nombre(), Fonts.HELVETICA_BOLD, 16,
                PdfTheme.WHITE, PdfCanvas.Align.CENTER);
        canvas.textAligned(40, 48, pageWidth - 80, titulo.toUpperCase(Locale.ROOT), Fonts.HELVETICA, 11,
                PdfTheme.HEADER_SUBTITLE, PdfCanvas.Align.CENTER);
        if (subtitulo != null && !subtitulo.isBlank()) {
            canvas.textAligned(40, 66, pageWidth - 80, subtitulo, Fonts.HELVETICA, 9,
                    PdfTheme.HEADER_SUBSUBTITLE, PdfCanvas.Align.CENTER);
        }
        canvas.rect(0, 90, pageWidth, 4, PdfTheme.ACCENT);

        return 110;
    }

    public static void drawFooter(PdfCanvas canvas, PdfTheme theme) throws IOException {
        float pageWidth = canvas.pageWidth();
        float y = canvas.pageHeight() - 40;

        canvas.rect(0, y - 8, pageWidth, 1, PdfTheme.BORDER);
        String fecha = LocalDate.now().format(FOOTER_DATE_FORMAT);
        canvas.text(40, y, theme.ciudad() + " — Generado el " + fecha, Fonts.HELVETICA, 8, PdfTheme.MUTED);
        canvas.textAligned(40, y, pageWidth - 80, "Documento generado por SGA", Fonts.HELVETICA, 8,
                PdfTheme.MUTED, PdfCanvas.Align.RIGHT);
    }

    public static float drawSectionTitle(PdfCanvas canvas, String title, float y) throws IOException {
        canvas.rect(40, y, canvas.pageWidth() - 80, 20, PdfTheme.LIGHT);
        canvas.rect(40, y, 4, 20, PdfTheme.SECONDARY);
        canvas.text(50, y + 5, title, Fonts.HELVETICA_BOLD, 10, PdfTheme.PRIMARY);
        return y + 28;
    }

    public static void drawField(PdfCanvas canvas, String label, String value, float x, float y) throws IOException {
        drawField(canvas, label, value, x, y, 130);
    }

    public static void drawField(PdfCanvas canvas, String label, String value, float x, float y, float labelWidth) throws IOException {
        canvas.text(x, y, label + ":", Fonts.HELVETICA_BOLD, 9, PdfTheme.MUTED);
        canvas.text(x + labelWidth, y, (value == null || value.isBlank()) ? "—" : value, Fonts.HELVETICA, 9, PdfTheme.DARK);
    }
}
