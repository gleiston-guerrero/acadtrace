package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.dto.EstudianteRequest;
import ec.uteq.sga.secretaria.dto.ImportacionEstudianteRow;
import ec.uteq.sga.secretaria.dto.ImportacionResultado;
import ec.uteq.sga.secretaria.dto.MatriculaRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Importación masiva de estudiantes (CSV/Excel/PDF listado CAS), calcada de
 * ImportacionExcelService de sga-principal. Solo parsea y valida aquí; la
 * escritura real de cada fila la hace EstudianteService.crear(...), que ya
 * está migrado a gRPC (código, cifrado, y persistencia en sga-principal).
 */
@Service
public class ImportacionEstudiantesService {

    private static final Set<String> COLUMNAS_REQUERIDAS = Set.of("CEDULA", "NOMBRES", "APELLIDOS");

    private final NamedParameterJdbcTemplate jdbc;
    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;

    public ImportacionEstudiantesService(NamedParameterJdbcTemplate jdbc, EstudianteService estudianteService,
                                          MatriculaService matriculaService) {
        this.jdbc = jdbc;
        this.estudianteService = estudianteService;
        this.matriculaService = matriculaService;
    }

    public ImportacionResultado parsearArchivo(MultipartFile archivo) {
        if (archivo.isEmpty())
            throw ApiException.badRequest("El archivo está vacío");

        String nombreArchivo = Optional.ofNullable(archivo.getOriginalFilename()).orElse("").toLowerCase();
        List<Map<String, String>> filas;

        try {
            if (nombreArchivo.endsWith(".csv")) {
                filas = leerCsv(archivo);
            } else if (nombreArchivo.endsWith(".xlsx") || nombreArchivo.endsWith(".xls")) {
                filas = leerExcel(archivo);
            } else if (nombreArchivo.endsWith(".pdf")) {
                filas = leerPdf(archivo);
            } else {
                throw ApiException.badRequest("Formato no soportado. Sube un archivo .csv, .xlsx, .xls o .pdf");
            }
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer el archivo: " + e.getMessage());
        }

        if (filas.isEmpty())
            throw ApiException.badRequest("El archivo no contiene filas de datos");

        List<ImportacionEstudianteRow> estudiantes = new ArrayList<>();
        Set<String> cedulasEnArchivo = new HashSet<>();
        int fila = 1;

        for (Map<String, String> registro : filas) {
            fila++;
            String cedula = registro.getOrDefault("CEDULA", "").trim();
            String nombres = registro.getOrDefault("NOMBRES", "").trim();
            String apellidos = registro.getOrDefault("APELLIDOS", "").trim();
            String correo = registro.getOrDefault("CORREO", "").trim();

            if (cedula.isEmpty() && nombres.isEmpty() && apellidos.isEmpty()) continue;

            String error = null;
            if (!cedula.matches("\\d{10}")) {
                error = "Cédula inválida (debe tener 10 dígitos)";
            } else if (nombres.isEmpty() || apellidos.isEmpty()) {
                error = "Nombres o apellidos vacíos";
            } else if (!cedulasEnArchivo.add(cedula)) {
                error = "Cédula duplicada en el archivo";
            }

            boolean yaExiste = error == null && existeCedula(cedula);

            ImportacionEstudianteRow row = new ImportacionEstudianteRow();
            row.setFila(fila);
            row.setCedula(cedula);
            row.setNombres(nombres);
            row.setApellidos(apellidos);
            row.setCorreo(correo);
            row.setYaExiste(yaExiste);
            row.setError(error);
            estudiantes.add(row);
        }

        if (estudiantes.isEmpty())
            throw ApiException.badRequest("El archivo no contiene filas de datos");

        int conError = (int) estudiantes.stream().filter(e -> e.getError() != null).count();

        return new ImportacionResultado(estudiantes, estudiantes.size(), estudiantes.size() - conError, conError);
    }

    private boolean existeCedula(String cedula) {
        List<Long> ids = jdbc.query(
                "SELECT id_estudiante FROM sga_secretaria.estudiantes WHERE cedula = :cedula",
                new MapSqlParameterSource("cedula", cedula),
                (rs, n) -> rs.getLong("id_estudiante"));
        return !ids.isEmpty();
    }

    public Map<String, Object> confirmarImportacion(List<ImportacionEstudianteRow> filas, String username,
                                                      Long idGrado, Long idParalelo, Long idAnoLectivo) {
        boolean matricular = idGrado != null && idParalelo != null && idAnoLectivo != null;
        int creados = 0, existentes = 0, omitidos = 0, matriculados = 0;

        for (ImportacionEstudianteRow fila : filas) {
            if (fila.getError() != null) { omitidos++; continue; }
            if (fila.isYaExiste()) { existentes++; continue; }

            String correo = fila.getCorreo() == null || fila.getCorreo().isBlank() ? null : fila.getCorreo();
            EstudianteRequest dto = new EstudianteRequest(
                    fila.getCedula(),       // cedula
                    fila.getNombres(),      // nombres
                    fila.getApellidos(),    // apellidos
                    null,                   // fecha_nacimiento
                    null,                   // genero
                    correo,                 // correo
                    null,                   // direccion
                    null,                   // telefono
                    null,                   // discapacidad
                    null,                   // tipo_discapacidad
                    null,                   // porcentaje_disc
                    null,                   // id_representante
                    null,                   // nacionalidad
                    null,                   // etnia
                    null,                   // lugar_nacimiento
                    null,                   // vive_con
                    null,                   // numeros_hermanos
                    null,                   // beneficio_social
                    null,                   // carnet_conadis
                    null);                  // foto_url
            try {
                Map<String, Object> creado = estudianteService.crear(dto, username);
                creados++;

                if (matricular) {
                    try {
                        Long idEstudiante = ((Number) creado.get("id_estudiante")).longValue();
                        matriculaService.crear(new MatriculaRequest(idEstudiante, idGrado, idParalelo, idAnoLectivo, null, null), username);
                        matriculados++;
                    } catch (ApiException ignored) {
                        // El estudiante ya quedó creado; la matrícula se puede completar luego desde el módulo de Matrículas.
                    }
                }
            } catch (ApiException e) {
                omitidos++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("creados", creados);
        resultado.put("existentes", existentes);
        resultado.put("omitidos", omitidos);
        resultado.put("matriculados", matriculados);
        resultado.put("total", filas.size());
        return resultado;
    }

    private List<Map<String, String>> leerCsv(MultipartFile archivo) throws IOException {
        List<Map<String, String>> filas = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            Map<String, String> mapaEncabezados = mapearEncabezados(parser.getHeaderNames());
            validarEncabezados(mapaEncabezados);

            for (CSVRecord record : parser) {
                Map<String, String> fila = new HashMap<>();
                mapaEncabezados.forEach((original, canonico) -> fila.put(canonico, record.get(original)));
                filas.add(fila);
            }
        }
        return filas;
    }

    private List<Map<String, String>> leerExcel(MultipartFile archivo) throws IOException {
        List<Map<String, String>> filas = new ArrayList<>();
        DataFormatter formatter = new DataFormatter();

        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Row encabezado = sheet.getRow(sheet.getFirstRowNum());
            if (encabezado == null)
                throw ApiException.badRequest("El archivo no tiene fila de encabezado");

            Map<Integer, String> columnas = new LinkedHashMap<>();
            List<String> nombresEncabezado = new ArrayList<>();
            for (int c = encabezado.getFirstCellNum(); c < encabezado.getLastCellNum(); c++) {
                String valor = formatter.formatCellValue(encabezado.getCell(c));
                nombresEncabezado.add(valor);
                columnas.put(c, valor);
            }
            Map<String, String> mapaEncabezados = mapearEncabezados(nombresEncabezado);
            validarEncabezados(mapaEncabezados);

            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Map<String, String> fila = new HashMap<>();
                for (Map.Entry<Integer, String> col : columnas.entrySet()) {
                    String canonico = mapaEncabezados.get(col.getValue());
                    if (canonico == null) continue;
                    fila.put(canonico, formatter.formatCellValue(row.getCell(col.getKey())).trim());
                }
                filas.add(fila);
            }
        }
        return filas;
    }

    private List<Map<String, String>> leerPdf(MultipartFile archivo) throws IOException {
        String texto;
        try (PDDocument doc = Loader.loadPDF(archivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            texto = stripper.getText(doc);
        }

        texto = texto.replaceAll("(?i)(\\.edu\\.e)\\s*[\\r\\n]+\\s*(c)", "$1$2");
        String[] lineas = texto.split("\\r?\\n");

        List<Map<String, String>> filas = new ArrayList<>();

        Pattern p1 = Pattern.compile(
                "^\\s*(\\d{1,3})\\s+(\\d{10})\\s+(.+?)\\s+([a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec)\\s*$");
        Pattern p2 = Pattern.compile(
                "^\\s*(\\d{1,3})\\s+(\\d{10})\\s+([A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ\\s]{5,})\\s*$");
        Pattern p3 = Pattern.compile(
                "(\\d{10})\\s+([A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ\\s]{5,}?)\\s*([a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec)?\\s*$");

        for (String linea : lineas) {
            if (linea.trim().isEmpty()) continue;
            if (linea.contains("CÉDULA") || linea.contains("CEDULA") || linea.contains("NOMBRES COMPLETOS")) continue;
            if (linea.contains("LISTADO") || linea.contains("Transformar")) continue;
            if (linea.contains("Institución") || linea.contains("Régimen") || linea.contains("Jornada")) continue;
            if (linea.contains("Año Escolar") || linea.contains("Año Lectivo") || linea.contains("Paralelo:")) continue;
            if (linea.contains("CUENTA") || linea.contains("No.")) continue;

            Map<String, String> fila = null;

            Matcher m1 = p1.matcher(linea);
            if (m1.matches()) {
                fila = construirFilaPdf(m1.group(2), m1.group(3).trim(), m1.group(4).trim());
            }
            if (fila == null) {
                Matcher m2 = p2.matcher(linea);
                if (m2.matches()) fila = construirFilaPdf(m2.group(2), m2.group(3).trim(), "");
            }
            if (fila == null) {
                Matcher m3 = p3.matcher(linea);
                if (m3.find()) {
                    String email = m3.group(3) != null ? m3.group(3).trim() : "";
                    fila = construirFilaPdf(m3.group(1), m3.group(2).trim(), email);
                }
            }
            if (fila != null) filas.add(fila);
        }

        List<String> emailsEncontrados = new ArrayList<>();
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec");
        for (String linea : lineas) {
            if (linea.contains("CUENTA") || linea.contains("CORREO")) continue;
            Matcher em = emailPattern.matcher(linea);
            while (em.find()) emailsEncontrados.add(em.group());
        }

        int emailIdx = 0;
        for (Map<String, String> fila : filas) {
            if (emailIdx >= emailsEncontrados.size()) break;
            if (fila.get("CORREO") == null || fila.get("CORREO").isBlank()) {
                fila.put("CORREO", emailsEncontrados.get(emailIdx));
            }
            emailIdx++;
        }

        return filas;
    }

    private Map<String, String> construirFilaPdf(String cedula, String nombresCompletos, String email) {
        nombresCompletos = nombresCompletos.replaceAll("\\s+", " ").trim();
        String[] partes = nombresCompletos.split("\\s+");
        String apellidos;
        String nombres;
        if (partes.length >= 4) {
            apellidos = partes[0] + " " + partes[1];
            nombres = String.join(" ", Arrays.copyOfRange(partes, 2, partes.length));
        } else if (partes.length == 3) {
            apellidos = partes[0] + " " + partes[1];
            nombres = partes[2];
        } else if (partes.length == 2) {
            apellidos = partes[0];
            nombres = partes[1];
        } else {
            apellidos = nombresCompletos;
            nombres = "";
        }

        Map<String, String> fila = new HashMap<>();
        fila.put("CEDULA", cedula);
        fila.put("APELLIDOS", apellidos);
        fila.put("NOMBRES", nombres);
        fila.put("CORREO", email);
        return fila;
    }

    private Map<String, String> mapearEncabezados(List<String> encabezados) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (String original : encabezados) {
            String normalizado = normalizar(original);
            String canonico = switch (normalizado) {
                case "CEDULA" -> "CEDULA";
                case "NOMBRES" -> "NOMBRES";
                case "APELLIDOS" -> "APELLIDOS";
                case "CORREO", "EMAIL", "CORREO ELECTRONICO" -> "CORREO";
                default -> null;
            };
            if (canonico != null) mapa.put(original, canonico);
        }
        return mapa;
    }

    private void validarEncabezados(Map<String, String> mapaEncabezados) {
        Set<String> encontrados = new HashSet<>(mapaEncabezados.values());
        if (!encontrados.containsAll(COLUMNAS_REQUERIDAS))
            throw ApiException.badRequest("El archivo debe tener las columnas CEDULA, NOMBRES y APELLIDOS");
    }

    private String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.trim().toUpperCase();
    }
}
