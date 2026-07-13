package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.AsignacionRepository;
import ec.edu.uteq.sga.repository.MatriculaRepository;
import ec.edu.uteq.sga.repository.PersonaRepository;
import ec.edu.uteq.sga.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherAuthorizationService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final AsignacionRepository asignacionRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnoLectivoRepository anoLectivoRepository;

    /**
     * Resuelve el docente autenticado a partir del SecurityContext.
     */
    public Persona getAuthenticatedTeacher() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No hay usuario autenticado en el contexto");
        }

        String username = auth.getName();
        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));

        boolean isDocente = usuario.getRoles().stream()
                .anyMatch(r -> r.getNombre().equalsIgnoreCase("DOCENTE") || r.getNombre().equalsIgnoreCase("ROLE_DOCENTE"));

        if (!isDocente) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario autenticado no tiene rol de DOCENTE");
        }

        return personaRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe un perfil de Persona (Docente) asociado a este usuario"));
    }

    /**
     * Obtiene las asignaciones de un docente.
     */
    public List<Asignacion> getTeacherAssignments(Long idDocente) {
        return asignacionRepository.findByDocente_IdPersona(idDocente);
    }

    /**
     * Valida que una asignación pertenezca al docente y esté activa.
     */
    public Asignacion validateTeacherAssignment(Long idDocente, Long idAsignacion) {
        Asignacion asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));

        if (!asignacion.getDocente().getIdPersona().equals(idDocente)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La asignación no pertenece al docente indicado");
        }

        if (!asignacion.isActivo()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "La asignación está inactiva");
        }

        return asignacion;
    }

    /**
     * Obtiene los estudiantes de una asignación.
     */
    public List<Matricula> getStudentsByAssignment(Long idAsignacion) {
        Asignacion asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        
        return matriculaRepository.findByGradoAndAnoLectivoWithEstudiante(
                asignacion.getGrado().getIdGrado(),
                asignacion.getAnoLectivo().getIdAnoLectivo()
        ).stream().filter(m -> "ACTIVA".equalsIgnoreCase(m.getEstado())).toList();
    }

    /**
     * Valida que una matrícula pertenezca al curso indicado por la asignación.
     */
    public Matricula validateStudentEnrollment(Long idMatricula, Long idAsignacion) {
        Asignacion asignacion = asignacionRepository.findById(idAsignacion)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));

        Matricula matricula = matriculaRepository.findById(idMatricula)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Matrícula no encontrada"));

        if (!matricula.getEstado().equalsIgnoreCase("ACTIVA")) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "La matrícula no está activa");
        }

        if (!matricula.getGrado().getIdGrado().equals(asignacion.getGrado().getIdGrado()) ||
            !matricula.getAnoLectivo().getIdAnoLectivo().equals(asignacion.getAnoLectivo().getIdAnoLectivo())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El estudiante no pertenece al grado y año lectivo de esta asignación");
        }

        return matricula;
    }

    /**
     * Obtiene el año lectivo actual.
     */
    public AnoLectivo getCurrentAcademicYear() {
        return anoLectivoRepository.findByEsActualTrue()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay un año lectivo activo configurado"));
    }
}
