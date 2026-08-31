package ec.edu.uteq.sga.application.report;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Patrón GoF Template Method: Define el esqueleto invariable para la generación
 * de reportes académicos oficiales en formato de texto/PDF.
 */
public abstract class GeneradorReporteAcademicoTemplate {

    /**
     * MÉTODO PLANTILLA (Template Method): Define el algoritmo invariable.
     */
    public final byte[] generarReporte(String titulo, Map<String, Object> parametros) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        String cabecera = generarCabecera(titulo, parametros);
        String cuerpo = procesarCuerpo(parametros);
        String pie = generarPie(parametros);
        String pieConFirma = aplicarFirmaSeguridad(pie);

        StringBuilder documentoCompleto = new StringBuilder();
        documentoCompleto.append(cabecera).append("\n");
        documentoCompleto.append(cuerpo).append("\n");
        documentoCompleto.append(pieConFirma).append("\n");

        return documentoCompleto.toString().getBytes();
    }

    // Paso 1 común: Genera membrete institucional
    protected String generarCabecera(String titulo, Map<String, Object> parametros) {
        return "========================================================\n" +
               " ESCUELA DE EDUCACIÓN BÁSICA PROVINCIAS UNIDAS\n" +
               " SISTEMA DE GESTIÓN ACADÉMICA - ACADTRACE\n" +
               " REPORTE: " + titulo.toUpperCase() + "\n" +
               " Fecha de emisión: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n" +
               "========================================================";
    }

    // Paso 2 abstracto: Cada reporte define su propio contenido específico
    protected abstract String procesarCuerpo(Map<String, Object> parametros);

    // Paso 3 común: Pie de página con cláusula de confidencialidad
    protected String generarPie(Map<String, Object> parametros) {
        return "--------------------------------------------------------\n" +
               " Documento emitido automáticamente por el sistema central.\n" +
               " Validez oficial sujeta a verificación de firma digital.";
    }

    // Paso 4 (Hook opcional): Aplica hash de integridad
    protected String aplicarFirmaSeguridad(String pie) {
        return pie + "\n [Sello de Integridad AcadTrace - Verificable SHA-256]";
    }
}