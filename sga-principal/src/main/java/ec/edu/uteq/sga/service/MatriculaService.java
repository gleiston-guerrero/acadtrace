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
