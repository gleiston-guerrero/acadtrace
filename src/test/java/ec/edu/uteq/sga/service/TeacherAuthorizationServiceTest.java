package ec.edu.uteq.sga.service;

import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TeacherAuthorizationServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private AsignacionRepository asignacionRepository;
    @Mock
    private MatriculaRepository matriculaRepository;
    @Mock
    private AnoLectivoRepository anoLectivoRepository;

    @InjectMocks
    private TeacherAuthorizationService teacherAuthorizationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedTeacher_Success() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan", "pass", java.util.List.of())
        );

        Rol rolDocente = Rol.builder().nombre("DOCENTE").build();
        Usuario usuario = Usuario.builder().idUsuario(1L).username("juan").roles(Set.of(rolDocente)).build();
        Persona persona = Persona.builder().idPersona(10L).usuario(usuario).nombres("Juan").build();

        when(usuarioRepository.findByUsername("juan")).thenReturn(Optional.of(usuario));
        when(personaRepository.findByUsuario_IdUsuario(1L)).thenReturn(Optional.of(persona));

        Persona result = teacherAuthorizationService.getAuthenticatedTeacher();

        assertNotNull(result);
        assertEquals(10L, result.getIdPersona());
    }

    @Test
    void getAuthenticatedTeacher_NoAuth_ThrowsException() {
        assertThrows(ResponseStatusException.class, () -> teacherAuthorizationService.getAuthenticatedTeacher());
    }

    @Test
    void getAuthenticatedTeacher_NotDocente_ThrowsException() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass", java.util.List.of())
        );

        Rol rolAdmin = Rol.builder().nombre("ADMIN").build();
        Usuario usuario = Usuario.builder().idUsuario(1L).username("admin").roles(Set.of(rolAdmin)).build();

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(usuario));

        assertThrows(ResponseStatusException.class, () -> teacherAuthorizationService.getAuthenticatedTeacher());
    }

    @Test
    void validateTeacherAssignment_Success() {
        Persona docente = Persona.builder().idPersona(10L).build();
        Asignacion asignacion = Asignacion.builder().idAsignacion(5L).docente(docente).activo(true).build();

        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        Asignacion result = teacherAuthorizationService.validateTeacherAssignment(10L, 5L);

        assertNotNull(result);
        assertEquals(5L, result.getIdAsignacion());
    }

    @Test
    void validateTeacherAssignment_WrongTeacher_ThrowsException() {
        Persona docente = Persona.builder().idPersona(20L).build(); // Not 10L
        Asignacion asignacion = Asignacion.builder().idAsignacion(5L).docente(docente).activo(true).build();

        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        assertThrows(ResponseStatusException.class, () -> teacherAuthorizationService.validateTeacherAssignment(10L, 5L));
    }

    @Test
    void validateTeacherAssignment_Inactive_ThrowsException() {
        Persona docente = Persona.builder().idPersona(10L).build();
        Asignacion asignacion = Asignacion.builder().idAsignacion(5L).docente(docente).activo(false).build(); // Inactive

        when(asignacionRepository.findById(5L)).thenReturn(Optional.of(asignacion));

        assertThrows(ResponseStatusException.class, () -> teacherAuthorizationService.validateTeacherAssignment(10L, 5L));
    }
}
