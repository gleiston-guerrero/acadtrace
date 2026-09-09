package ec.edu.uteq.sga.application.service;

import ec.edu.uteq.sga.domain.dto.usuario.UsuarioCreacionResponseDTO;
import ec.edu.uteq.sga.domain.dto.usuario.UsuarioRequestDTO;
import ec.edu.uteq.sga.domain.dto.usuario.UsuarioResponseDTO;
import ec.edu.uteq.sga.domain.entity.Persona;
import ec.edu.uteq.sga.domain.entity.Rol;
import ec.edu.uteq.sga.domain.entity.Usuario;
import ec.edu.uteq.sga.infrastructure.repository.PersonaRepository;
import ec.edu.uteq.sga.infrastructure.repository.RolRepository;
import ec.edu.uteq.sga.infrastructure.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: UsuarioService (SGA Principal)")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private RolRepository rolRepo;

    @Mock
    private PersonaRepository personaRepo;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @Mock
    private AuditoriaService auditoriaService;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuario;
    private Rol rolDocente;
    private UsuarioRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        rolDocente = Rol.builder()
                .idRol(2L)
                .nombre("DOCENTE")
                .build();

        usuario = Usuario.builder()
                .idUsuario(1L)
                .username("ccastro")
                .correo("ccastro@uteq.edu.ec")
                .passwordHash("hashed_password")
                .estado(true)
                .primerIngreso(true)
                .roles(Set.of(rolDocente))
                .build();

        requestDTO = new UsuarioRequestDTO();
        requestDTO.setNombres("Carlos");
        requestDTO.setApellidos("Castro");
        requestDTO.setCorreo("ccastro@uteq.edu.ec");
        requestDTO.setRoles(Set.of(2L));
    }

    // ─── 1. LISTADO Y BÚSQUEDA ───────────────────────────────────────────────
    @Test
    @DisplayName("1. Listar todos los usuarios — Retorna lista de DTOs")
    void listarTodos_retornaLista() {
        given(usuarioRepo.findAll()).willReturn(List.of(usuario));
        given(personaRepo.findByUsuario_IdUsuario(1L)).willReturn(Optional.empty());

        List<UsuarioResponseDTO> lista = usuarioService.listarTodos();

        assertThat(lista).isNotEmpty();
        assertThat(lista.get(0).getUsername()).isEqualTo("ccastro");
    }

    @Test
    @DisplayName("2. Obtener por ID — Existe — Retorna DTO")
    void obtenerPorId_whenExiste_retornaDTO() {
        given(usuarioRepo.findById(1L)).willReturn(Optional.of(usuario));
        given(personaRepo.findByUsuario_IdUsuario(1L)).willReturn(Optional.empty());

        UsuarioResponseDTO dto = usuarioService.obtenerPorId(1L);

        assertThat(dto).isNotNull();
        assertThat(dto.getUsername()).isEqualTo("ccastro");
    }

    @Test
    @DisplayName("3. Obtener por ID — No existe — Lanza 404 NOT_FOUND")
    void obtenerPorId_whenNoExiste_throwsNotFound() {
        given(usuarioRepo.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.obtenerPorId(999L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404 NOT_FOUND");
    }

    // ─── 2. CREACIÓN ────────────────────────────────────────────────────────
    @Test
    @DisplayName("4. Crear usuario — Datos válidos — Genera username, contraseña y envía correo")
    void crear_whenValido_guardaYEnviaCorreo() {
        given(usuarioRepo.existsByCorreo("ccastro@uteq.edu.ec")).willReturn(false);
        given(usuarioRepo.existsByUsername(anyString())).willReturn(false);
        given(rolRepo.findById(2L)).willReturn(Optional.of(rolDocente));
        given(passwordEncoder.encode(anyString())).willReturn("encoded_pwd");
        given(usuarioRepo.save(any(Usuario.class))).willReturn(usuario);

        UsuarioCreacionResponseDTO result = usuarioService.crear(requestDTO);

        assertThat(result).isNotNull();
        assertThat(result.getCorreo()).isEqualTo("ccastro@uteq.edu.ec");
        then(emailService).should(times(1)).enviarCredenciales(anyString(), anyString(), anyString(), anyString());
        then(auditoriaService).should(times(1)).registrarCrud(any(), any(), any(), any());
    }

    @Test
    @DisplayName("5. Crear usuario — Correo duplicado — Lanza 409 CONFLICT")
    void crear_whenCorreoDuplicado_throwsConflict() {
        given(usuarioRepo.existsByCorreo("ccastro@uteq.edu.ec")).willReturn(true);

        assertThatThrownBy(() -> usuarioService.crear(requestDTO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        then(usuarioRepo).should(never()).save(any());
    }

    // ─── 3. GESTIÓN DE ESTADO Y RESETEO ─────────────────────────────────────
    @Test
    @DisplayName("6. Resetear contraseña — Genera nuevo password y notifica")
    void resetearPassword_whenExiste_reseteaYNotifica() {
        given(usuarioRepo.findById(1L)).willReturn(Optional.of(usuario));
        given(passwordEncoder.encode(anyString())).willReturn("new_encoded_pwd");
        given(usuarioRepo.save(any(Usuario.class))).willReturn(usuario);

        usuarioService.resetearPassword(1L);

        assertThat(usuario.isPrimerIngreso()).isTrue();
        then(emailService).should(times(1)).enviarCredenciales(anyString(), anyString(), anyString(), anyString());
        then(auditoriaService).should(times(1)).registrarConfig(any(), any(), anyLong(), any());
    }

    @Test
    @DisplayName("7. Eliminar usuario — Existe — Elimina y audita")
    void eliminar_whenExiste_eliminaYAudita() {
        given(usuarioRepo.findById(1L)).willReturn(Optional.of(usuario));

        usuarioService.eliminar(1L);

        then(usuarioRepo).should(times(1)).deleteById(1L);
        then(auditoriaService).should(times(1)).registrarCrud(any(), any(), anyLong(), any());
    }
}
