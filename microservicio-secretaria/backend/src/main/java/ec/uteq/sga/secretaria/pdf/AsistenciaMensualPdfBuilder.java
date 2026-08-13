package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static ec.uteq.sga.secretaria.pdf.PdfValues.formatFechaEc;
import static ec.uteq.sga.secretaria.pdf.PdfValues.str;

public final class AsistenciaMensualPdfBuilder {

    private record Col(String label, float x, float w) {}

    private static final List<Col> COLS = List.of(
            new Col("Fecha", 40, 90),
            new Col("Materia", 130, 300),
            new Col("Estado", 430, 90),
            new Col("Justificación", 520, 280)
    );

    /** A4 landscape: se invierte el rectangulo en vez de usar page.setRotation, para dibujar sin transforms. */
    private static final PDRectangle A4_LANDSCAPE = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());

    private static final Map<String, Color> COLOR_POR_ESTADO = Map.of(
            "PRESENTE", Color.decode("#1b7a3d"),
            "AUSENTE", Color.decode("#c0392b"),
            "ATRASO", Color.decode("#b8860b"),
            "JUSTIFICADO", Color.decode("#2e86ab")
    );

    private AsistenciaMensualPdfBuilder() {}

    public static byte[] build(Map<String, Object> matricula, String mesLabel,
                                List<Map<String, Object>> registros, PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(A4_LANDSCAPE);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                String subtitulo = mesLabel + " · " + str(matricula.get("estudiante"));
                float y = PdfLayout.drawHeader(canvas, theme, "Registro Mensual de Asistencia", subtitulo) + 8;

                PdfLayout.drawField(canvas, "Estudiante", str(matricula.get("estudiante")), 40, y);
                PdfLayout.drawField(canvas, "Cédula", str(matricula.get("cedula")), 320, y);
                PdfLayout.drawField(canvas, "Grado / Curso",
                        "%s \"%s\"".formatted(str(matricula.get("grado")), str(matricula.get("paralelo"))), 560, y, 90);
                y += 24;

                canvas.rect(40, y, canvas.pageWidth() - 80, 18, PdfTheme.SECONDARY);
                for (Col c : COLS) {
                    canvas.text(c.x(), y + 5, c.label(), Fonts.HELVETICA_BOLD, 8, PdfTheme.WHITE);
                }
                y += 20;

                int i = 0;
                for (Map<String, Object> row : registros) {
                    if (y > canvas.pageHeight() - 60) {
                        PdfLayout.drawFooter(canvas, theme);
                        PDPage next = new PDPage(A4_LANDSCAPE);
                        document.addPage(next);
                        canvas.newPage(next);
                        y = 30;
                    }

                    Color bg = (i % 2 == 0) ? PdfTheme.WHITE : PdfTheme.LIGHT;
                    canvas.rect(40, y, canvas.pageWidth() - 80, 16, bg);

                    String estado = str(row.get("estado"));
                    String materia = canvas.truncate(str(row.get("asignatura")), Fonts.HELVETICA, 8, COLS.get(1).w() - 4);
                    String justificacion = canvas.truncate(str(row.get("justificacion")), Fonts.HELVETICA, 8, COLS.get(3).w() - 4);

                    canvas.text(COLS.get(0).x(), y + 4, formatFechaEc(row.get("fecha")), Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(1).x(), y + 4, materia, Fonts.HELVETICA, 8, PdfTheme.DARK);
                    canvas.text(COLS.get(2).x(), y + 4, estado, Fonts.HELVETICA_BOLD, 8, COLOR_POR_ESTADO.getOrDefault(estado, PdfTheme.DARK));
                    canvas.text(COLS.get(3).x(), y + 4, justificacion, Fonts.HELVETICA, 8, PdfTheme.MUTED);

                    y += 16;
                    i++;
                }

                if (registros.isEmpty()) {
                    canvas.text(40, y + 4, "No hay registros de asistencia para este mes.", Fonts.HELVETICA, 9, PdfTheme.MUTED);
                    y += 20;
                }

                canvas.rect(40, y + 4, canvas.pageWidth() - 80, 1, PdfTheme.BORDER);
                canvas.text(40, y + 8, "Total de registros: " + registros.size(), Fonts.HELVETICA_BOLD, 9, PdfTheme.PRIMARY);
                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
