package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ec.uteq.sga.secretaria.pdf.PdfValues.formatFechaEc;
import static ec.uteq.sga.secretaria.pdf.PdfValues.str;

public final class NominaMatriculasPdfBuilder {

    private record Col(String label, float x, float w) {}

    private static final List<Col> COLS = List.of(
            new Col("N°", 40, 30),
            new Col("Cédula", 70, 80),
            new Col("Apellidos y Nombres", 150, 200),
            new Col("Grado", 350, 80),
            new Col("Paralelo", 430, 60),
            new Col("Estado", 490, 70),
            new Col("F. Matrícula", 560, 80)
    );

    /** A4 landscape: se invierte el rectangulo en vez de usar page.setRotation, para dibujar sin transforms. */
    private static final PDRectangle A4_LANDSCAPE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private NominaMatriculasPdfBuilder() {}

    public static byte[] build(List<Map<String, Object>> datos, String anoLectivo, String grado, String paralelo,
                                PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(A4_LANDSCAPE);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                String subtitulo = Stream.of(anoLectivo, grado, (paralelo != null && !paralelo.isBlank()) ? "Paralelo " + paralelo : null)
                        .filter(s -> s != null && !s.isBlank())
                        .collect(Collectors.joining(" · "));
                float y = PdfLayout.drawHeader(canvas, theme, "Nómina de Estudiantes Matriculados", subtitulo) + 8;

                canvas.rect(40, y, canvas.pageWidth() - 80, 18, PdfTheme.SECONDARY);
                for (Col c : COLS) {
                    canvas.text(c.x(), y + 5, c.label(), Fonts.HELVETICA_BOLD, 8, PdfTheme.WHITE);
                }
                y += 20;

                int i = 0;
                for (Map<String, Object> row : datos) {
                    if (y > canvas.pageHeight() - 60) {
                        PdfLayout.drawFooter(canvas, theme);
                        PDPage next = new PDPage(A4_LANDSCAPE);
                        document.addPage(next);
                        canvas.newPage(next);
                        y = 30;
                    }

                    Color bg = (i % 2 == 0) ? PdfTheme.WHITE : PdfTheme.LIGHT;
                    canvas.rect(40, y, canvas.pageWidth() - 80, 16, bg);

                    String estudiante = canvas.truncate(str(row.get("estudiante")), Fonts.HELVETICA, 8, COLS.get(2).w() - 4);
                    canvas.text(COLS.get(0).x(), y + 4, String.valueOf(i + 1), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(1).x(), y + 4, str(row.get("cedula")), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(2).x(), y + 4, estudiante, Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(3).x(), y + 4, str(row.get("grado")), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(4).x(), y + 4, str(row.get("paralelo")), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(5).x(), y + 4, str(row.get("estado")), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(6).x(), y + 4, formatFechaEc(row.get("fecha_registro")), Fonts.HELVETICA, 8, PdfTheme.DARK);

                    y += 16;
                    i++;
                }

                canvas.rect(40, y + 4, canvas.pageWidth() - 80, 1, PdfTheme.BORDER);
                canvas.text(40, y + 8, "Total de estudiantes: " + datos.size(), Fonts.HELVETICA_BOLD, 9, PdfTheme.PRIMARY);
                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
