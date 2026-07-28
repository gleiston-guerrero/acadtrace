package ec.uteq.sga.secretaria.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.awt.Color;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper sobre PDPageContentStream con coordenadas top-left (como pdfkit), para poder portar
 * pdfGenerator.js casi mecanicamente. PDFBox usa origen bottom-left (Y crece hacia arriba); esta
 * clase hace el flip internamente en cada llamada.
 */
public class PdfCanvas implements Closeable {

    public enum Align { LEFT, CENTER, RIGHT }

    private final PDDocument document;
    private PDPage page;
    private PDPageContentStream stream;

    public PdfCanvas(PDDocument document, PDPage page) throws IOException {
        this.document = document;
        this.page = page;
        this.stream = new PDPageContentStream(document, page);
    }

    public float pageWidth() {
        return page.getMediaBox().getWidth();
    }

    public float pageHeight() {
        return page.getMediaBox().getHeight();
    }

    /** Cierra el content stream de la pagina actual y continua dibujando en una nueva (paginacion de tablas). */
    public void newPage(PDPage newPage) throws IOException {
        stream.close();
        this.page = newPage;
        this.stream = new PDPageContentStream(document, newPage);
    }

    public void rect(float x, float yTop, float width, float height, Color color) throws IOException {
        stream.setNonStrokingColor(color);
        stream.addRect(x, pageHeight() - yTop - height, width, height);
        stream.fill();
    }

    /** Texto sin alineacion/ajuste, ancla top-left en (x, yTop) igual que doc.text(x,y) en pdfkit. */
    public void text(float x, float yTop, String value, PDFont font, float size, Color color) throws IOException {
        drawLine(x, yTop, safe(value), font, size, color);
    }

    /** Texto de una sola linea alineado dentro de una banda [x, x+width]. */
    public void textAligned(float x, float yTop, float width, String value, PDFont font, float size,
                             Color color, Align align) throws IOException {
        String text = safe(value);
        float textWidth = widthOf(text, font, size);
        float drawX = switch (align) {
            case LEFT -> x;
            case CENTER -> x + Math.max(0, (width - textWidth) / 2f);
            case RIGHT -> x + Math.max(0, width - textWidth);
        };
        drawLine(drawX, yTop, text, font, size, color);
    }

    /** Recorta el texto con "…" si excede maxWidth (evita que una celda invada la columna siguiente). */
    public String truncate(String value, PDFont font, float size, float maxWidth) throws IOException {
        String text = safe(value);
        if (widthOf(text, font, size) <= maxWidth) return text;
        String ellipsis = "…";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            String candidate = sb.toString() + text.charAt(i) + ellipsis;
            if (widthOf(candidate, font, size) > maxWidth) break;
            sb.append(text.charAt(i));
        }
        return sb + ellipsis;
    }

    /**
     * Parrafo multi-linea ajustado a `width`. Simplificacion aceptada: el "align: justify" de pdfkit
     * (usado en un unico parrafo introductorio) se reduce a texto envuelto alineado (left/center),
     * sin estirar espacios entre palabras.
     */
    public float textWrapped(float x, float yTop, float width, String value, PDFont font, float size,
                              Color color, float lineHeight, Align align) throws IOException {
        List<String> lines = wrap(safe(value), font, size, width);
        float y = yTop;
        for (String line : lines) {
            textAligned(x, y, width, line, font, size, color, align);
            y += lineHeight;
        }
        return y;
    }

    private void drawLine(float x, float yTop, String text, PDFont font, float size, Color color) throws IOException {
        if (text.isEmpty()) return;
        float baseline = pageHeight() - yTop - ascent(font, size);
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, baseline);
        stream.showText(text);
        stream.endText();
    }

    private List<String> wrap(String text, PDFont font, float size, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder current = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = current.isEmpty() ? word : current + " " + word;
                if (!current.isEmpty() && widthOf(candidate, font, size) > maxWidth) {
                    lines.add(current.toString());
                    current = new StringBuilder(word);
                } else {
                    current = new StringBuilder(candidate);
                }
            }
            lines.add(current.toString());
        }
        return lines;
    }

    private float widthOf(String text, PDFont font, float size) throws IOException {
        return font.getStringWidth(text) / 1000f * size;
    }

    private float ascent(PDFont font, float size) {
        return font.getFontDescriptor().getAscent() / 1000f * size;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
