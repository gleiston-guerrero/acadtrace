package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.asignacion.*;
import ec.edu.uteq.sga.domain.entity.*;
import ec.edu.uteq.sga.infrastructure.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignacionRepo;
    private final PersonaRepository personaRepo;
    private final AsignaturaRepository asignaturaRepo;
    private final GradoRepository gradoRepo;
    private final ParaleloRepository paraleloRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final UsuarioRepository usuarioRepo;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarDocentes() {
        return usuarioRepo.findAll().stream()
                .filter(u -> u.getRoles().stream()
                        .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE")))
                .map(u -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idDocente", u.getIdUsuario());
                    String nombre = personaRepo.findByUsuario_IdUsuario(u.getIdUsuario())
                            .map(p -> p.getNombres() + " " + p.getApellidos())
                            .orElse(u.getUsername());
                    map.put("nombre", nombre);
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarParalelosPorGrado(Long idGrado) {
        return paraleloRepo.findByGradoIdGradoAndActivoTrueOrderByLetra(idGrado).stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("idParalelo", p.getIdParalelo());
                    map.put("letra", p.getLetra());
                    return map;
                }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> listarTodos() {
        return asignacionRepo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> listarPorAnoLectivo(Long idAnoLectivo) {
        return asignacionRepo.findByAnoLectivo_IdAnoLectivo(idAnoLectivo)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AsignacionResponseDTO> listarPorDocente(Long idDocente) {
        return asignacionRepo.findByDocente_IdPersona(idDocente)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public AsignacionResponseDTO crear(AsignacionRequestDTO dto, String username) {
        if (asignacionRepo.existsByAsignatura_IdAsignaturaAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivo(
                dto.getIdAsignatura(), dto.getIdParalelo(), dto.getIdAnoLectivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta asignatura ya fue asignada a este curso y paralelo en el año lectivo seleccionado.");
        }

        Persona docente = personaRepo.findById(dto.getIdDocente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Docente no encontrado"));

        Asignatura asignatura = asignaturaRepo.findById(dto.getIdAsignatura())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asignatura no encontrada"));

        Grado grado = gradoRepo.findById(dto.getIdGrado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grado no encontrado"));

        Paralelo paralelo = paraleloRepo.findById(dto.getIdParalelo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paralelo no encontrado"));

        AnoLectivo anoLectivo = anoLectivoRepo.findById(dto.getIdAnoLectivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Año lectivo no encontrado"));

        Usuario asignadoPor = usuarioRepo.findByUsername(username)
                .or(() -> usuarioRepo.findAll().stream().filter(u -> u.getUsername().equalsIgnoreCase(username)).findFirst())
                .orElse(null);

        int horasNuevas = dto.getHorasSemanales() != null && dto.getHorasSemanales() > 0 ? dto.getHorasSemanales() : 4;
        int horasActualesGrado = asignacionRepo.findAll().stream()
                .filter(a -> a.getGrado().getIdGrado().equals(dto.getIdGrado())
                        && a.getAnoLectivo().getIdAnoLectivo().equals(dto.getIdAnoLectivo())
                        && a.isActivo())
                .mapToInt(a -> a.getHorasSemanales() != null ? a.getHorasSemanales() : 4)
                .sum();

        if (horasActualesGrado + horasNuevas > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Exceso de carga horaria: El curso ya suma " + horasActualesGrado + "h. Asignar " + horasNuevas + "h superaría el límite máximo de 30 horas semanales.");
        }

        Asignacion asignacion = Asignacion.builder()
                .docente(docente)
                .asignatura(asignatura)
                .grado(grado)
                .paralelo(paralelo)
                .anoLectivo(anoLectivo)
                .esTutor(dto.isEsTutor())
                .activo(true)
                .horasSemanales(dto.getHorasSemanales() != null && dto.getHorasSemanales() > 0 ? dto.getHorasSemanales() : 4)
                .fechaAsignacion(java.time.Instant.now())
                .asignadoPor(asignadoPor)
                .build();

        try {
            return toDTO(asignacionRepo.save(asignacion));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede guardar: la asignatura o tutoría ya está asignada para este curso.");
        }
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Asignacion asignacion = asignacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        asignacion.setActivo(activo);
        asignacionRepo.save(asignacion);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!asignacionRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada");
        }
        try {
            jdbcTemplate.update("DELETE FROM sga_docente.promedios_anuales WHERE id_asignacion = ?", id);
            jdbcTemplate.update("DELETE FROM sga_docente.promedios_trimestrales WHERE id_asignacion = ?", id);
            jdbcTemplate.update("DELETE FROM sga_docente.resumen_asistencia WHERE id_asignacion = ?", id);
            jdbcTemplate.update("DELETE FROM sga_docente.asistencias WHERE id_asignacion = ?", id);
            jdbcTemplate.update("DELETE FROM sga_docente.actividades WHERE id_asignacion = ?", id);
            jdbcTemplate.update("DELETE FROM sga_principal.horarios WHERE id_asignacion = ?", id);
            asignacionRepo.deleteById(id);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo eliminar la asignación: " + ex.getMessage());
        }
    }

    @Transactional
    public AsignacionResponseDTO actualizar(Long id, AsignacionRequestDTO dto) {
        Asignacion asignacion = asignacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));

        Persona docente = personaRepo.findById(dto.getIdDocente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Docente no encontrado"));
        Asignatura asignatura = asignaturaRepo.findById(dto.getIdAsignatura())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Asignatura no encontrada"));
        Grado grado = gradoRepo.findById(dto.getIdGrado())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Grado no encontrado"));
        Paralelo paralelo = paraleloRepo.findById(dto.getIdParalelo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paralelo no encontrado"));
        AnoLectivo anoLectivo = anoLectivoRepo.findById(dto.getIdAnoLectivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Año lectivo no encontrado"));

        asignacion.setDocente(docente);
        asignacion.setAsignatura(asignatura);
        asignacion.setGrado(grado);
        asignacion.setParalelo(paralelo);
        asignacion.setAnoLectivo(anoLectivo);
        asignacion.setEsTutor(dto.isEsTutor());
        if (dto.getHorasSemanales() != null && dto.getHorasSemanales() > 0) {
            asignacion.setHorasSemanales(dto.getHorasSemanales());
        }

        try {
            return toDTO(asignacionRepo.save(asignacion));
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se pudo actualizar: la asignatura o tutoría entra en conflicto con otra asignación existente.");
        }
    }

    private AsignacionResponseDTO toDTO(Asignacion a) {
        return AsignacionResponseDTO.builder()
                .idAsignacion(a.getIdAsignacion())
                .docente(a.getDocente().getNombres() + " " + a.getDocente().getApellidos())
                .asignatura(a.getAsignatura().getNombre())
                .grado(a.getGrado().getNombre())
                .paralelo(a.getParalelo() != null ? a.getParalelo().getLetra() : null)
                .anoLectivo(a.getAnoLectivo().getNombre())
                .esTutor(a.isEsTutor())
                .activo(a.isActivo())
                .fechaAsignacion(a.getFechaAsignacion())
                .asignadoPor(a.getAsignadoPor() != null ? a.getAsignadoPor().getUsername() : null)
                .idDocente(a.getDocente().getIdPersona())
                .idAsignatura(a.getAsignatura().getIdAsignatura())
                .idGrado(a.getGrado().getIdGrado())
                .idParalelo(a.getParalelo() != null ? a.getParalelo().getIdParalelo() : null)
                .idAnoLectivo(a.getAnoLectivo().getIdAnoLectivo())
                .cedulaDocente(a.getDocente().getCedula())
                .correoDocente(a.getDocente().getUsuario() != null ? a.getDocente().getUsuario().getCorreo() : null)
                .tituloDocente(a.getDocente().getTituloAcademico())
                .fotoDocente(a.getDocente().getFotoUrl())
                .horasSemanales(a.getHorasSemanales() != null ? a.getHorasSemanales() : 4)
                .build();
    }
}