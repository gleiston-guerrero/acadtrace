package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.importacion.*;
import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DEUDA TECNICA CONOCIDA: mismo bypass que ImportacionCasService (escribe
 * estudiantes/matriculas por JPA directo, sin pasar por sga-secretaria).
 * Se acepta a proposito para este flujo de carga masiva inicial; ver el
 * comentario de ImportacionCasService para el detalle completo.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacionExcelService {

    private static final Set<String> COLUMNAS_REQUERIDAS = Set.of("CEDULA", "NOMBRES", "APELLIDOS");

    private final EstudianteRepository estudianteRepo;
    private final GradoRepository gradoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final ParaleloRepository paraleloRepo;

    public ExcelImportResultDTO parsearArchivo(MultipartFile archivo) {
        if (archivo.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");

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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Formato no soportado. Sube un archivo .csv, .xlsx, .xls o .pdf");
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo: " + e.getMessage());
        }

        if (filas.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no contiene filas de datos");

        List<ExcelEstudianteDTO> estudiantes = new ArrayList<>();
        Set<String> cedulasEnArchivo = new HashSet<>();
        int fila = 1;

        for (Map<String, String> registro : filas) {
            fila++;
            String cedula = registro.getOrDefault("CEDULA", "").trim();
            String nombres = registro.getOrDefault("NOMBRES", "").trim();
            String apellidos = registro.getOrDefault("APELLIDOS", "").trim();
            String correo = registro.getOrDefault("CORREO", "").trim();

            if (cedula.isEmpty() && nombres.isEmpty() && apellidos.isEmpty()) continue; // fila en blanco

            String error = null;
            if (!cedula.matches("\\d{10}")) {
                error = "Cédula inválida (debe tener 10 dígitos)";
            } else if (nombres.isEmpty() || apellidos.isEmpty()) {
                error = "Nombres o apellidos vacíos";
            } else if (!cedulasEnArchivo.add(cedula)) {
                error = "Cédula duplicada en el archivo";
            }

            boolean yaExiste = error == null && estudianteRepo.existsByCedula(cedula);

            estudiantes.add(ExcelEstudianteDTO.builder()
                    .fila(fila)
                    .cedula(cedula)
                    .nombres(nombres)
                    .apellidos(apellidos)
                    .email(correo)
                    .yaExiste(yaExiste)
                    .error(error)
                    .build());
        }

        if (estudiantes.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no contiene filas de datos");

        int conError = (int) estudiantes.stream().filter(e -> e.getError() != null).count();

        return ExcelImportResultDTO.builder()
                .estudiantes(estudiantes)
                .totalFilas(estudiantes.size())
                .filasValidas(estudiantes.size() - conError)
                .filasConError(conError)
                .build();
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no tiene fila de encabezado");

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

    /**
     * Reutiliza las mismas 3 estrategias regex de ImportacionCasService para extraer
     * filas de un listado CAS en PDF, pero devolviendo el mismo formato Map<String,String>
     * que leerCsv/leerExcel para que el resto del pipeline (validación, dedupe, yaExiste)
     * sea idéntico sin importar el origen del archivo.
     */
    private List<Map<String, String>> leerPdf(MultipartFile archivo) throws IOException {
        String texto;
        try (PDDocument doc = Loader.loadPDF(archivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            texto = stripper.getText(doc);
        }

        // Unir líneas que son continuación de un email partido (ej: .edu.e\nc → .edu.ec)
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

        // Segundo paso: recolectar todos los emails del texto y asignarlos por orden
        // (PDFBox suele extraer la columna CUENTA en líneas separadas)
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

    /** Mapea cada encabezado tal cual viene en el archivo a su nombre canónico (CEDULA/NOMBRES/APELLIDOS/CORREO). */
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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El archivo debe tener las columnas CEDULA, NOMBRES y APELLIDOS");
    }

    private String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinAcentos.trim().toUpperCase();
    }

    @Transactional
    public Map<String, Object> confirmarImportacion(ConfirmarImportacionExcelDTO dto) {
        if (dto.getIdParalelo() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un paralelo");

        Grado grado = gradoRepo.findById(dto.getIdGrado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado"));
        AnoLectivo anoLectivo = anoLectivoRepo.findById(dto.getIdAnoLectivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Año lectivo no encontrado"));
        Paralelo paralelo = paraleloRepo.findById(dto.getIdParalelo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paralelo no encontrado"));

        int creados = 0;
        int existentes = 0;
        int matriculados = 0;
        int omitidos = 0;

        for (ExcelEstudianteDTO est : dto.getEstudiantes()) {
            if (est.getError() != null) {
                omitidos++;
                continue;
            }

            Optional<Estudiante> existente = estudianteRepo.findByCedula(est.getCedula());
            Estudiante estudiante;

            if (existente.isPresent()) {
                estudiante = existente.get();
                existentes++;
            } else {
                estudiante = Estudiante.builder()
                        .cedula(est.getCedula())
                        .apellidos(est.getApellidos())
                        .nombres(est.getNombres())
                        .correo(est.getEmail())
                        .estado("ACTIVA")
                        .origenListado("EXCEL")
                        .nacionalidad("Ecuatoriana")
                        .fechaCreacion(Instant.now())
                        .fechaActualizacion(Instant.now())
                        .build();
                estudianteRepo.save(estudiante);
                creados++;
            }

            boolean yaMatriculado = matriculaRepo.existsByEstudiante_IdEstudianteAndAnoLectivo_IdAnoLectivo(
                    estudiante.getIdEstudiante(), anoLectivo.getIdAnoLectivo());

            if (!yaMatriculado) {
                Matricula matricula = Matricula.builder()
                        .estudiante(estudiante)
                        .grado(grado)
                        .paralelo(paralelo)
                        .anoLectivo(anoLectivo)
                        .estado("ACTIVA")
                        .build();
                matriculaRepo.save(matricula);
                matriculados++;
            }
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("creados", creados);
        resultado.put("existentes", existentes);
        resultado.put("matriculados", matriculados);
        resultado.put("omitidos", omitidos);
        resultado.put("total", dto.getEstudiantes().size());
        return resultado;
    }
}
