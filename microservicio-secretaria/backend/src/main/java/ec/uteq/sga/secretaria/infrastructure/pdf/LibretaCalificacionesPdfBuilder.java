package ec.uteq.sga.secretaria.infrastructure.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static ec.uteq.sga.secretaria.infrastructure.pdf.PdfValues.str;

public final class LibretaCalificacionesPdfBuilder {

    private record Col(String label, float x, float w) {}

    private static final List<Col> COLS = List.of(
            new Col("Materia", 40, 205),
            new Col("P. Formativo", 245, 75),
            new Col("N. Sumativa", 325, 75),
            new Col("P. Trimestral", 405, 75),
            new Col("Cualitativa", 485, 65)
    );

    private LibretaCalificacionesPdfBuilder() {}

    public static byte[] build(Map<String, Object> matricula, Map<String, Object> periodo,
                                List<Map<String, Object>> materias, PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                String subtitulo = str(periodo.get("nombre")) + " · Año Lectivo " + str(matricula.get("ano_lectivo"));
                float y = PdfLayout.drawHeader(canvas, theme, "Libreta de Calificaciones", subtitulo) + 10;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS DEL ESTUDIANTE", y);
                PdfLayout.drawField(canvas, "Nombres completos", str(matricula.get("estudiante")), 40, y);
                PdfLayout.drawField(canvas, "Cédula", str(matricula.get("cedula")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Grado / Curso",
                        "%s \"%s\"".formatted(str(matricula.get("grado")), str(matricula.get("paralelo"))), 40, y);
                PdfLayout.drawField(canvas, "Período", str(periodo.get("nombre")), 320, y);
                y += 30;

                y = PdfLayout.drawSectionTitle(canvas, "CALIFICACIONES POR MATERIA", y);

                canvas.rect(40, y, canvas.pageWidth() - 80, 18, PdfTheme.SECONDARY);
                for (Col c : COLS) {
                    canvas.text(c.x(), y + 5, c.label(), Fonts.HELVETICA_BOLD, 8, PdfTheme.WHITE);
                }
                y += 20;

                int i = 0;
                for (Map<String, Object> row : materias) {
                    if (y > canvas.pageHeight() - 60) {
                        PdfLayout.drawFooter(canvas, theme);
                        PDPage next = new PDPage(PDRectangle.A4);
                        document.addPage(next);
                        canvas.newPage(next);
                        y = 30;
                    }

                    Color bg = (i % 2 == 0) ? PdfTheme.WHITE : PdfTheme.LIGHT;
                    canvas.rect(40, y, canvas.pageWidth() - 80, 16, bg);

                    String materia = canvas.truncate(str(row.get("asignatura")), Fonts.HELVETICA, 8, COLS.get(0).w() - 4);
                    canvas.text(COLS.get(0).x(), y + 4, materia, Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.textAligned(COLS.get(1).x(), y + 4, COLS.get(1).w() - 6, str(row.get("promedio_formativo")), Fonts.HELVETICA, 8, PdfTheme.DARK, PdfCanvas.Align.RIGHT);
                    canvas.textAligned(COLS.get(2).x(), y + 4, COLS.get(2).w() - 6, str(row.get("nota_sumativa")), Fonts.HELVETICA, 8, PdfTheme.DARK, PdfCanvas.Align.RIGHT);
                    canvas.textAligned(COLS.get(3).x(), y + 4, COLS.get(3).w() - 6, str(row.get("promedio_trimestral")), Fonts.HELVETICA_BOLD, 8, PdfTheme.DARK, PdfCanvas.Align.RIGHT);
                    canvas.textAligned(COLS.get(4).x(), y + 4, COLS.get(4).w() - 6, str(row.get("nota_cualitativa")), Fonts.HELVETICA, 8, PdfTheme.DARK, PdfCanvas.Align.RIGHT);

                    y += 16;
                    i++;
                }

                if (materias.isEmpty()) {
                    canvas.text(40, y + 4, "No hay calificaciones registradas para este período.", Fonts.HELVETICA, 9, PdfTheme.MUTED);
                    y += 20;
                }

                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
