package ec.uteq.sga.secretaria.infrastructure.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static ec.uteq.sga.secretaria.infrastructure.pdf.PdfValues.str;

public final class FichaRepresentantePdfBuilder {

    private FichaRepresentantePdfBuilder() {}

    public static byte[] build(Map<String, Object> representante, PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                String nombreCompleto = (str(representante.get("nombres")) + " " + str(representante.get("apellidos"))).trim();
                float y = PdfLayout.drawHeader(canvas, theme, "Ficha del Representante", nombreCompleto) + 10;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS PERSONALES", y);
                PdfLayout.drawField(canvas, "Nombres", str(representante.get("nombres")), 40, y);
                PdfLayout.drawField(canvas, "Apellidos", str(representante.get("apellidos")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Cédula", str(representante.get("cedula")), 40, y);
                PdfLayout.drawField(canvas, "Parentesco", str(representante.get("parentesco")), 320, y);
                y += 30;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS DE CONTACTO", y);
                PdfLayout.drawField(canvas, "Teléfono principal", str(representante.get("telefono_principal")), 40, y);
                PdfLayout.drawField(canvas, "Teléfono alterno", str(representante.get("telefono_alt")), 320, y);
                y += 20;
                PdfLayout.drawField(canvas, "Correo electrónico", str(representante.get("correo")), 40, y);
                y += 20;
                y = canvas.textWrapped(40, y, canvas.pageWidth() - 80,
                        "Dirección: " + str(representante.get("direccion")), Fonts.HELVETICA, 9, PdfTheme.DARK, 13f, PdfCanvas.Align.LEFT);
                y += 20;

                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
