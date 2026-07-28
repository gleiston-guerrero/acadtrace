package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static ec.uteq.sga.secretaria.pdf.PdfValues.formatFechaEc;
import static ec.uteq.sga.secretaria.pdf.PdfValues.str;

public final class CertificadoMatriculaPdfBuilder {

    private CertificadoMatriculaPdfBuilder() {}

    public static byte[] build(Map<String, Object> matricula, PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                String anoLectivo = str(matricula.get("ano_lectivo"));
                float y = PdfLayout.drawHeader(canvas, theme, "Certificado de Matrícula", "Año Lectivo " + anoLectivo) + 10;

                String intro = ("La secretaría de %s certifica que el/la estudiante cuyos datos se detallan a " +
                        "continuación se encuentra legalmente matriculado/a en esta institución educativa:")
                        .formatted(theme.nombre());
                y = canvas.textWrapped(40, y, canvas.pageWidth() - 80, intro, Fonts.HELVETICA, 10,
                        PdfTheme.TEXT, 13f, PdfCanvas.Align.LEFT);
                y += 40;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS DEL ESTUDIANTE", y);
                String nombresEstudiante = str(matricula.get("nombres_estudiante"));
                if (nombresEstudiante.isBlank()) nombresEstudiante = str(matricula.get("estudiante"));
                PdfLayout.drawField(canvas, "Nombres completos", nombresEstudiante, 40, y);
                PdfLayout.drawField(canvas, "Cédula / Pasaporte", str(matricula.get("cedula")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Código estudiantil", str(matricula.get("codigo_estudiante")), 40, y);
                y += 30;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS DE MATRÍCULA", y);
                PdfLayout.drawField(canvas, "Año lectivo", anoLectivo, 40, y);
                PdfLayout.drawField(canvas, "Estado", str(matricula.get("estado")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Grado / Curso",
                        "%s \"%s\"".formatted(str(matricula.get("grado")), str(matricula.get("paralelo"))), 40, y);
                PdfLayout.drawField(canvas, "N° de matrícula", str(matricula.get("numero_orden")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Fecha de matrícula", formatFechaEc(matricula.get("fecha_registro")), 40, y);
                y += 40;

                canvas.rect(40, y, canvas.pageWidth() - 80, 1, PdfTheme.BORDER);
                y += 10;
                y = canvas.textWrapped(40, y, canvas.pageWidth() - 80,
                        "El presente certificado se expide a petición del interesado/a para los fines legales que estime conveniente.",
                        Fonts.HELVETICA, 9, PdfTheme.MUTED, 12f, PdfCanvas.Align.CENTER);
                y += 30;

                float firmaX = canvas.pageWidth() / 2 - 75;
                canvas.rect(firmaX, y, 150, 1, PdfTheme.DARK);
                y += 6;
                canvas.textAligned(firmaX, y, 150, "Secretaría", Fonts.HELVETICA_BOLD, 9, PdfTheme.DARK, PdfCanvas.Align.CENTER);
                canvas.textAligned(firmaX, y + 12, 150, theme.nombre(), Fonts.HELVETICA, 9, PdfTheme.MUTED, PdfCanvas.Align.CENTER);

                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
