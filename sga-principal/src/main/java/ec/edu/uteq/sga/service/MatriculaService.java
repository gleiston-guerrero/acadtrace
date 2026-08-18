package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.matricula.MatriculaRequestDTO;
import ec.edu.uteq.sga.dto.matricula.MatriculaResponseDTO;
import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/**
 * Matricula: entidad JPA propia de sga-principal (unico duenio de la tabla
 * compartida sga_principal.matriculas). Secretaria la gestiona por completo
 * via gRPC (ver PrincipalGrpcService), nunca por SQL directo.
 */
@Service
@RequiredArgsConstructor
public class MatriculaService {

    private static final Set<String> ESTADOS_VALIDOS = Set.of("ACTIVA", "RETIRADA", "TRASLADADA", "PROMOVIDA", "REPROBADA");

    private final MatriculaRepository matriculaRepo;
    private final EstudianteRepository estudianteRepo;
    private final GradoRepository gradoRepo;
    private final ParaleloRepository paraleloRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final UsuarioRepository usuarioRepo;
    private final AuditoriaService auditoriaService;

    public record PaginaMatriculas(List<MatriculaResponseDTO> items, long total) {}

    @Transactional(readOnly = true)
    public PaginaMatriculas listar(Long idAnoLectivo, Long idEstudiante, String q, int page, int limit) {
        if (idEstudiante != null && idEstudiante > 0) {
            List<Matricula> filas = matriculaRepo.findByEstudiante_IdEstudiante(idEstudiante);
            if (idAnoLectivo != null && idAnoLectivo > 0) {
                filas = filas.stream().filter(m -> m.getAnoLectivo().getIdAnoLectivo().equals(idAnoLectivo)).toList();
            }
            List<MatriculaResponseDTO> items = filas.stream().map(this::toDTO).toList();
            return new PaginaMatriculas(items, items.size());
        }

        if (idAnoLectivo == null || idAnoLectivo <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar idAnoLectivo o idEstudiante");

        List<Matricula> todas = matriculaRepo.findByAnoLectivoWithEstudiante(idAnoLectivo);
        if (q != null && !q.isBlank()) {
            String needle = q.trim().toLowerCase();
            todas = todas.stream().filter(m -> coincide(m.getEstudiante(), needle)).toList();
        }

        int total = todas.size();
        int limiteReal = limit > 0 ? limit : 20;
        int paginaReal = page > 0 ? page : 1;
        int desde = Math.min((paginaReal - 1) * limiteReal, total);
        int hasta = Math.min(desde + limiteReal, total);
        List<MatriculaResponseDTO> items = todas.subList(desde, hasta).stream().map(this::toDTO).toList();
        return new PaginaMatriculas(items, total);
    }

    private boolean coincide(Estudiante e, String needle) {
        return contiene(e.getNombres(), needle) || contiene(e.getApellidos(), needle) || contiene(e.getCedula(), needle);
    }

    private boolean contiene(String valor, String needle) {
        return valor != null && valor.toLowerCase().contains(needle);
    }

    @Transactional(readOnly = true)
    public MatriculaResponseDTO obtener(Long id) {
        return toDTO(buscarPorId(id));
    }

    @Transactional
    public MatriculaResponseDTO crear(MatriculaRequestDTO dto, Long idUsuarioRegistro) {
        Estudiante estudiante = estudianteRepo.findById(dto.getIdEstudiante())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Estudiante no encontrado"));
        Grado grado = gradoRepo.findById(dto.getIdGrado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no encontrado"));
        AnoLectivo ano = anoLectivoRepo.findById(dto.getIdAnoLectivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Año lectivo no encontrado"));
        if (dto.getIdParalelo() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El paralelo es obligatorio");
        Paralelo paralelo = paraleloRepo.findById(dto.getIdParalelo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paralelo no encontrado"));

        if (matriculaRepo.existsByEstudiante_IdEstudianteAndAnoLectivo_IdAnoLectivo(dto.getIdEstudiante(), dto.getIdAnoLectivo()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya tiene una matrícula en ese año lectivo");

        String estado = validarEstado(dto.getEstado(), "ACTIVA");

        short siguienteOrden = (short) (matriculaRepo.findTopByAnoLectivo_IdAnoLectivoOrderByNumeroOrdenDesc(dto.getIdAnoLectivo())
                .map(Matricula::getNumeroOrden)
                .orElse((short) 0) + 1);

        Usuario registrador = idUsuarioRegistro != null ? usuarioRepo.findById(idUsuarioRegistro).orElse(null) : null;

        Matricula matricula = Matricula.builder()
                .estudiante(estudiante)
                .grado(grado)
                .paralelo(paralelo)
                .anoLectivo(ano)
                .numeroOrden(siguienteOrden)
                .estado(estado)
                .observaciones(dto.getObservaciones())
                .registradoPor(registrador)
                .build();

        Matricula guardada = matriculaRepo.save(matricula);
        auditoriaService.registrarCrud("CREAR", "matricula", guardada.getIdMatricula(),
                "Matrícula creada: " + estudiante.getNombres() + " " + estudiante.getApellidos()
                        + " — " + grado.getNombre() + " (" + ano.getNombre() + ")");
        return toDTO(guardada);
    }

    @Transactional
    public void cambiarEstado(Long id, String estado) {
        Matricula matricula = buscarPorId(id);
        matricula.setEstado(validarEstado(estado, matricula.getEstado()));
        matriculaRepo.save(matricula);
        auditoriaService.registrarCrud("EDITAR", "matricula", id,
                "Estado de matrícula cambiado a " + matricula.getEstado());
    }

    private String validarEstado(String estado, String porDefecto) {
        if (estado == null || estado.isBlank()) return porDefecto;
        String limpio = estado.trim().toUpperCase();
        if (!ESTADOS_VALIDOS.contains(limpio))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado inválido: " + estado);
        return limpio;
    }

    private Matricula buscarPorId(Long id) {
        return matriculaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));
    }

    @Transactional(readOnly = true)
    public byte[] generarPdfMatricula(Long idMatricula) {
        Matricula m = buscarPorId(idMatricula);
        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            com.lowagie.text.Document doc = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4, 36, 36, 36, 36);
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, baos);
            doc.open();

            com.lowagie.text.Font fTitle = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 16, com.lowagie.text.Font.BOLD, new java.awt.Color(36, 58, 118));
            com.lowagie.text.Font fSub = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD, java.awt.Color.DARK_GRAY);
            com.lowagie.text.Font fLabel = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.BOLD, java.awt.Color.BLACK);
            com.lowagie.text.Font fValue = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10, com.lowagie.text.Font.NORMAL, java.awt.Color.DARK_GRAY);

            com.lowagie.text.Paragraph p1 = new com.lowagie.text.Paragraph("ESCUELA DE EDUCACIÓN BÁSICA PROVINCIAS UNIDAS", fTitle);
            p1.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            doc.add(p1);

            com.lowagie.text.Paragraph p2 = new com.lowagie.text.Paragraph("FICHA OFICIAL DE MATRÍCULA — AÑO LECTIVO " + m.getAnoLectivo().getNombre(), fSub);
            p2.setAlignment(com.lowagie.text.Element.ALIGN_CENTER);
            p2.setSpacingAfter(15);
            doc.add(p2);

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10);
            table.setSpacingAfter(20);

            addCell(table, "N° Folio / Orden:", "MAT-" + m.getAnoLectivo().getNombre() + "-" + String.format("%04d", m.getNumeroOrden() != null ? m.getNumeroOrden() : 1), fLabel, fValue);
            addCell(table, "Estudiante:", m.getEstudiante().getApellidos() + " " + m.getEstudiante().getNombres(), fLabel, fValue);
            addCell(table, "Cédula / Identificación:", m.getEstudiante().getCedula() != null ? m.getEstudiante().getCedula() : "—", fLabel, fValue);
            addCell(table, "Código Estudiante (CAS):", m.getEstudiante().getCodigoEstudiante() != null ? m.getEstudiante().getCodigoEstudiante() : "—", fLabel, fValue);
            addCell(table, "Grado Asignado:", m.getGrado().getNombre(), fLabel, fValue);
            addCell(table, "Paralelo:", m.getParalelo() != null ? "Paralelo " + m.getParalelo().getLetra() : "Paralelo A", fLabel, fValue);
            addCell(table, "Jornada:", "Matutina (07:30 - 12:30)", fLabel, fValue);
            addCell(table, "Estado de Matrícula:", m.getEstado(), fLabel, fValue);
            addCell(table, "Observaciones:", m.getObservaciones() != null ? m.getObservaciones() : "Ninguna", fLabel, fValue);

            doc.add(table);

            com.lowagie.text.Paragraph pFirmas = new com.lowagie.text.Paragraph("\n\n\n\n\n", fValue);
            doc.add(pFirmas);

            com.lowagie.text.pdf.PdfPTable fTable = new com.lowagie.text.pdf.PdfPTable(2);
            fTable.setWidthPercentage(100);
            com.lowagie.text.pdf.PdfPCell cellF1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("_____________________________\nFirma del Representante Legal\nC.I.: ", fValue));
            cellF1.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellF1.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);

            com.lowagie.text.pdf.PdfPCell cellF2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph("_____________________________\nSecretaría / Dirección Institucional\nEscuela Provincias Unidas", fValue));
            cellF2.setBorder(com.lowagie.text.Rectangle.NO_BORDER);
            cellF2.setHorizontalAlignment(com.lowagie.text.Element.ALIGN_CENTER);

            fTable.addCell(cellF1);
            fTable.addCell(cellF2);
            doc.add(fTable);

            doc.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al generar Ficha PDF: " + e.getMessage());
        }
    }

    private void addCell(com.lowagie.text.pdf.PdfPTable table, String label, String value, com.lowagie.text.Font fLabel, com.lowagie.text.Font fValue) {
        com.lowagie.text.pdf.PdfPCell c1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(label, fLabel));
        c1.setPadding(8);
        c1.setBackgroundColor(new java.awt.Color(245, 247, 250));
        com.lowagie.text.pdf.PdfPCell c2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Paragraph(value, fValue));
        c2.setPadding(8);
        table.addCell(c1);
        table.addCell(c2);
    }

    private MatriculaResponseDTO toDTO(Matricula m) {
        Estudiante e = m.getEstudiante();
        return MatriculaResponseDTO.builder()
                .idMatricula(m.getIdMatricula())
                .idEstudiante(e.getIdEstudiante())
                .estudianteNombres(e.getNombres())
                .estudianteApellidos(e.getApellidos())
                .estudianteCedula(e.getCedula())
                .estudianteCodigo(e.getCodigoEstudiante())
                .idGrado(m.getGrado().getIdGrado())
                .grado(m.getGrado().getNombre())
                .idParalelo(m.getParalelo() != null ? m.getParalelo().getIdParalelo() : null)
                .paralelo(m.getParalelo() != null ? m.getParalelo().getLetra() : null)
                .idAnoLectivo(m.getAnoLectivo().getIdAnoLectivo())
                .anoLectivo(m.getAnoLectivo().getNombre())
                .numeroOrden(m.getNumeroOrden())
                .fechaRegistro(m.getFechaRegistro())
                .estado(m.getEstado())
                .observaciones(m.getObservaciones())
                .registradoPor(m.getRegistradoPor() != null ? m.getRegistradoPor().getUsername() : null)
                .fechaCreacion(m.getFechaCreacion())
                .build();
    }
}
