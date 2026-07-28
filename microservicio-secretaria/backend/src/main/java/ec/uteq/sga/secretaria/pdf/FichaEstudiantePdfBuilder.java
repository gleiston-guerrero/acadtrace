package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

import static ec.uteq.sga.secretaria.pdf.PdfValues.formatFechaEc;
import static ec.uteq.sga.secretaria.pdf.PdfValues.str;

public final class FichaEstudiantePdfBuilder {

    private FichaEstudiantePdfBuilder() {}

    public static byte[] build(Map<String, Object> estudiante, PdfTheme theme) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PdfCanvas canvas = new PdfCanvas(document, page)) {
                float y = PdfLayout.drawHeader(canvas, theme, "Ficha del Estudiante", "") + 10;

                y = PdfLayout.drawSectionTitle(canvas, "DATOS PERSONALES", y);
                PdfLayout.drawField(canvas, "Nombres", str(estudiante.get("nombres")), 40, y);
                PdfLayout.drawField(canvas, "Apellidos", str(estudiante.get("apellidos")), 320, y);
                y += 18;
                PdfLayout.drawField(canvas, "Cédula", str(estudiante.get("cedula")), 40, y);
                PdfLayout.drawField(canvas, "Código", str(estudiante.get("codigo_estudiante")), 320, y);
                y += 18;
                PdfLayout.drawField(canvas, "Fecha nacimiento", formatFechaEc(estudiante.get("fecha_nacimiento")), 40, y);
                PdfLayout.drawField(canvas, "Género", str(estudiante.get("genero")), 320, y);
                y += 18;
                PdfLayout.drawField(canvas, "Teléfono", str(estudiante.get("telefono")), 40, y);
                PdfLayout.drawField(canvas, "Correo", str(estudiante.get("correo")), 320, y);
                y += 18;
                PdfLayout.drawField(canvas, "Dirección", str(estudiante.get("direccion")), 40, y, 100);
                y += 30;

                String repNombres = str(estudiante.get("rep_nombres"));
                if (!repNombres.isBlank()) {
                    y = PdfLayout.drawSectionTitle(canvas, "REPRESENTANTE LEGAL", y);
                    PdfLayout.drawField(canvas, "Nombres", repNombres + " " + str(estudiante.get("rep_apellidos")), 40, y);
                    PdfLayout.drawField(canvas, "Parentesco", str(estudiante.get("parentesco")), 320, y);
                    y += 18;
                    PdfLayout.drawField(canvas, "Cédula", str(estudiante.get("rep_cedula")), 40, y);
                    PdfLayout.drawField(canvas, "Teléfono", str(estudiante.get("rep_telefono")), 320, y);
                    y += 18;
                    PdfLayout.drawField(canvas, "Correo", str(estudiante.get("rep_correo")), 40, y);
                    y += 30;
                }

                String tipoSangre = str(estudiante.get("tipo_sangre"));
                String alergias = str(estudiante.get("alergias"));
                if (!tipoSangre.isBlank() || !alergias.isBlank()) {
                    y = PdfLayout.drawSectionTitle(canvas, "INFORMACIÓN MÉDICA", y);
                    PdfLayout.drawField(canvas, "Tipo de sangre", tipoSangre, 40, y);
                    PdfLayout.drawField(canvas, "Alergias", alergias, 320, y);
                    y += 18;
                    PdfLayout.drawField(canvas, "Medicación", str(estudiante.get("medicacion_permanente")), 40, y, 100);
                    y += 18;
                    PdfLayout.drawField(canvas, "Contacto emergencia", str(estudiante.get("contacto_emergencia")), 40, y);
                    PdfLayout.drawField(canvas, "Tel. emergencia", str(estudiante.get("telefono_emergencia")), 320, y);
                    y += 30;
                }

                Object discapacidad = estudiante.get("discapacidad");
                if (Boolean.TRUE.equals(discapacidad)) {
                    y = PdfLayout.drawSectionTitle(canvas, "DISCAPACIDAD", y);
                    PdfLayout.drawField(canvas, "Tipo", str(estudiante.get("tipo_discapacidad")), 40, y);
                    Object porcentaje = estudiante.get("porcentaje_disc");
                    PdfLayout.drawField(canvas, "Porcentaje", (porcentaje == null ? "0" : String.valueOf(porcentaje)) + "%", 320, y);
                    y += 30;
                }

                PdfLayout.drawFooter(canvas, theme);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
