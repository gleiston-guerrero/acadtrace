package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.dto.asignacion.*;
import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
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
        if (asignacionRepo.existsByDocente_IdPersonaAndAsignatura_IdAsignaturaAndGrado_IdGradoAndAnoLectivo_IdAnoLectivo(
                dto.getIdDocente(), dto.getIdAsignatura(), dto.getIdGrado(), dto.getIdAnoLectivo()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe esa asignación");

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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        Asignacion asignacion = Asignacion.builder()
                .docente(docente)
                .asignatura(asignatura)
                .grado(grado)
                .paralelo(paralelo)
                .anoLectivo(anoLectivo)
                .esTutor(dto.isEsTutor())
                .activo(true)
                .fechaAsignacion(java.time.Instant.now())
                .asignadoPor(asignadoPor)
                .build();

        return toDTO(asignacionRepo.save(asignacion));
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo) {
        Asignacion asignacion = asignacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        asignacion.setActivo(activo);
        asignacionRepo.save(asignacion);
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
                .build();
    }
}