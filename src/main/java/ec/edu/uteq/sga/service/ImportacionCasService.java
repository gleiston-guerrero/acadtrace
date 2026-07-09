package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.importacion.*;
import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportacionCasService {

    private final EstudianteRepository estudianteRepo;
    private final GradoRepository gradoRepo;
    private final MatriculaRepository matriculaRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final ParaleloRepository paraleloRepo;

    public CasPdfResultDTO parsearPdf(MultipartFile archivo) {
        if (archivo.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo está vacío");

        String texto;
        try (PDDocument doc = Loader.loadPDF(archivo.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            texto = stripper.getText(doc);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el PDF: " + e.getMessage());
        }

        log.info("=== TEXTO EXTRAÍDO DEL PDF ===\n{}", texto);

        // Unir líneas que son continuación de un email partido (ej: .edu.e\nc → .edu.ec)
        texto = texto.replaceAll("(?i)(\\.edu\\.e)\\s*[\\r\\n]+\\s*(c)", "$1$2");

        String[] lineas = texto.split("\\r?\\n");

        String anoEscolar = extraerCampo(lineas, "Año Escolar:", "Ano Escolar:");
        String paralelo = extraerCampo(lineas, "Paralelo:");
        String anoLectivo = extraerCampo(lineas, "Año Lectivo:", "Ano Lectivo:");
        String regimen = extraerCampo(lineas, "Régimen:", "Regimen:");
        String jornada = extraerCampo(lineas, "Jornada:");
        String institucion = extraerCampo(lineas, "Institución Educativa:", "Institucion Educativa:");

        List<CasEstudianteDTO> estudiantes = new ArrayList<>();

        // Estrategia 1: línea completa con email
        Pattern p1 = Pattern.compile(
            "^\\s*(\\d{1,3})\\s+(\\d{10})\\s+(.+?)\\s+([a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec)\\s*$"
        );

        // Estrategia 2: sin email (solo número + cédula + nombres)
        Pattern p2 = Pattern.compile(
            "^\\s*(\\d{1,3})\\s+(\\d{10})\\s+([A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ\\s]{5,})\\s*$"
        );

        // Estrategia 3: buscar cédulas sueltas con nombres en la misma línea
        Pattern p3 = Pattern.compile(
            "(\\d{10})\\s+([A-ZÁÉÍÓÚÑ][A-ZÁÉÍÓÚÑ\\s]{5,}?)\\s*([a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec)?\\s*$"
        );

        int fila = 0;

        for (String linea : lineas) {
            // Saltar cabeceras y líneas vacías
            if (linea.trim().isEmpty()) continue;
            if (linea.contains("CÉDULA") || linea.contains("CEDULA") || linea.contains("NOMBRES COMPLETOS")) continue;
            if (linea.contains("LISTADO") || linea.contains("Transformar")) continue;
            if (linea.contains("Institución") || linea.contains("Régimen") || linea.contains("Jornada")) continue;
            if (linea.contains("Año Escolar") || linea.contains("Año Lectivo") || linea.contains("Paralelo:")) continue;
            if (linea.contains("CUENTA") || linea.contains("No.")) continue;

            CasEstudianteDTO est = null;

            // Intentar estrategia 1
            Matcher m1 = p1.matcher(linea);
            if (m1.matches()) {
                fila = Integer.parseInt(m1.group(1));
                est = construirEstudiante(fila, m1.group(2), m1.group(3).trim(), m1.group(4).trim());
            }

            // Intentar estrategia 2
            if (est == null) {
                Matcher m2 = p2.matcher(linea);
                if (m2.matches()) {
                    fila = Integer.parseInt(m2.group(1));
                    est = construirEstudiante(fila, m2.group(2), m2.group(3).trim(), "");
                }
            }

            // Intentar estrategia 3
            if (est == null) {
                Matcher m3 = p3.matcher(linea);
                if (m3.find()) {
                    fila++;
                    String email = m3.group(3) != null ? m3.group(3).trim() : "";
                    est = construirEstudiante(fila, m3.group(1), m3.group(2).trim(), email);
                }
            }

            if (est != null) {
                estudiantes.add(est);
            }
        }

        // Segundo paso: recolectar TODOS los emails del texto y asignarlos por orden
        // PDFBox extrae la columna CUENTA en líneas separadas
        List<String> emailsEncontrados = new ArrayList<>();
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9._]+@[a-zA-Z0-9._]+\\.edu\\.ec");
        for (String linea : lineas) {
            if (linea.contains("CUENTA") || linea.contains("CORREO")) continue;
            Matcher em = emailPattern.matcher(linea);
            while (em.find()) {
                emailsEncontrados.add(em.group());
            }
        }
        log.info("Emails encontrados: {} | Estudiantes: {}", emailsEncontrados.size(), estudiantes.size());

        int emailIdx = 0;
        for (CasEstudianteDTO est : estudiantes) {
            if (emailIdx >= emailsEncontrados.size()) break;
            if (est.getEmail() == null || est.getEmail().isBlank()) {
                est.setEmail(emailsEncontrados.get(emailIdx));
            }
            emailIdx++;
        }

        if (estudiantes.isEmpty())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se encontraron estudiantes en el PDF. Verifica que sea un listado CAS válido.");

        return CasPdfResultDTO.builder()
                .anoEscolar(anoEscolar)
                .paralelo(paralelo)
                .anoLectivo(anoLectivo)
                .regimen(regimen)
                .jornada(jornada)
                .institucion(institucion)
                .estudiantes(estudiantes)
                .build();
    }

    private CasEstudianteDTO construirEstudiante(int fila, String cedula, String nombresCompletos, String email) {
        // Limpiar nombres
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

        boolean yaExiste = estudianteRepo.existsByCedula(cedula);

        return CasEstudianteDTO.builder()
                .fila(fila)
                .cedula(cedula)
                .apellidos(apellidos)
                .nombres(nombres)
                .email(email)
                .yaExiste(yaExiste)
                .build();
    }

    @Transactional
    public Map<String, Object> confirmarImportacion(ConfirmarImportacionDTO dto) {
        Grado grado = gradoRepo.findById(dto.getIdGrado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado"));
        AnoLectivo anoLectivo = anoLectivoRepo.findById(dto.getIdAnoLectivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Año lectivo no encontrado"));
        Paralelo paralelo = dto.getIdParalelo() != null
                ? paraleloRepo.findById(dto.getIdParalelo())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paralelo no encontrado"))
                : null;

        int creados = 0;
        int existentes = 0;
        int matriculados = 0;

        for (CasEstudianteDTO est : dto.getEstudiantes()) {
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
                        .origenListado("CAS")
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
        resultado.put("total", dto.getEstudiantes().size());
        return resultado;
    }

    private String extraerCampo(String[] lineas, String... claves) {
        for (String linea : lineas) {
            for (String clave : claves) {
                int idx = linea.indexOf(clave);
                if (idx >= 0) {
                    return linea.substring(idx + clave.length()).trim();
                }
            }
        }
        return "";
    }
}
