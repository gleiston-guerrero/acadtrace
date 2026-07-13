package ec.edu.uteq.sga.config;

import ec.edu.uteq.sga.entity.*;
import ec.edu.uteq.sga.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Component
@Profile("dev")
public class DataSeedRunner implements CommandLineRunner {

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private PersonaRepository personaRepository;
    @Autowired
    private RolRepository rolRepository;
    @Autowired
    private AnoLectivoRepository anoLectivoRepository;
    @Autowired
    private GradoRepository gradoRepository;
    @Autowired
    private AsignaturaRepository asignaturaRepository;
    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private EstudianteRepository estudianteRepository;
    @Autowired
    private MatriculaRepository matriculaRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("====== INICIANDO SEED DE DATOS (PERFIL DEV) ======");

        // 1. Crear Rol DOCENTE si no existe
        Rol rolDocente = rolRepository.findByNombre("ROLE_DOCENTE").orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre("ROLE_DOCENTE");
            nuevo.setDescripcion("Rol para los docentes del sistema");
            nuevo.setActivo(true);
            return rolRepository.save(nuevo);
        });

        // 2. Crear Usuario y Persona
        String username = "keyla123@correo.local";
        Usuario usuario = usuarioRepository.findByUsername(username).orElseGet(() -> {
            Usuario nuevo = new Usuario();
            nuevo.setUsername(username);
            nuevo.setCorreo(username);
            nuevo.setPasswordHash(passwordEncoder.encode("Keyla123@"));
            nuevo.setEstado(true);
            nuevo.setPrimerIngreso(false); // Para que no pida cambiar contraseña
            nuevo.getRoles().add(rolDocente);
            return usuarioRepository.save(nuevo);
        });

        Persona docente = personaRepository.findAll().stream().filter(p -> p.getUsuario().getIdUsuario().equals(usuario.getIdUsuario())).findFirst().orElseGet(() -> {
            Persona nueva = new Persona();
            nueva.setNombres("Keyla");
            nueva.setApellidos("Docente");
            nueva.setCedula("0000000001");
            nueva.setUsuario(usuario);
            return personaRepository.save(nueva);
        });

        // 3. Crear Año Lectivo
        AnoLectivo ano = anoLectivoRepository.findAll().stream().filter(a -> a.getNombre().equals("2026 - 2027")).findFirst().orElseGet(() -> {
            AnoLectivo nuevo = new AnoLectivo();
            nuevo.setNombre("2026 - 2027");
            nuevo.setFechaInicio(LocalDate.of(2026, 4, 1));
            nuevo.setFechaFin(LocalDate.of(2027, 2, 28));
            nuevo.setEsActual(true);
            return anoLectivoRepository.save(nuevo);
        });

        // 4. Crear Grado
        Grado grado = null;
        if (gradoRepository.findAll().stream().anyMatch(g -> g.getNombre().equals("Octavo de prueba"))) {
            grado = gradoRepository.findAll().stream().filter(g -> g.getNombre().equals("Octavo de prueba")).findFirst().get();
        } else {
            grado = new Grado();
            grado.setNombre("Octavo de prueba");
            grado.setOrden((short) 8);
            grado.setActivo(true);
            grado = gradoRepository.save(grado);
        }

        // 5. Crear Asignatura
        Asignatura asignatura = null;
        if (asignaturaRepository.findAll().stream().anyMatch(a -> a.getNombre().equals("Matemática de prueba"))) {
            asignatura = asignaturaRepository.findAll().stream().filter(a -> a.getNombre().equals("Matemática de prueba")).findFirst().get();
        } else {
            asignatura = new Asignatura();
            asignatura.setNombre("Matemática de prueba");
            asignatura.setActiva(true);
            asignatura = asignaturaRepository.save(asignatura);
        }

        // 6. Crear Asignación
        final Grado g = grado;
        final Asignatura a = asignatura;
        Asignacion asignacion = asignacionRepository.findAll()
                .stream()
                .filter(as -> as.getDocente().getIdPersona().equals(docente.getIdPersona()) && 
                              as.getAnoLectivo().getIdAnoLectivo().equals(ano.getIdAnoLectivo()) &&
                              as.getGrado().getIdGrado().equals(g.getIdGrado()) && 
                              as.getAsignatura().getIdAsignatura().equals(a.getIdAsignatura()))
                .findFirst()
                .orElseGet(() -> {
                    Asignacion nueva = new Asignacion();
                    nueva.setDocente(docente);
                    nueva.setAnoLectivo(ano);
                    nueva.setGrado(g);
                    nueva.setAsignatura(a);
                    nueva.setActivo(true);
                    return asignacionRepository.save(nueva);
                });

        // 7. Crear Estudiantes
        Estudiante est1 = estudianteRepository.findByCedula("0000000002").orElseGet(() -> {
            Estudiante e = new Estudiante();
            e.setNombres("Estudiante Prueba");
            e.setApellidos("Uno");
            e.setCedula("0000000002");
            return estudianteRepository.save(e);
        });

        Estudiante est2 = estudianteRepository.findByCedula("0000000003").orElseGet(() -> {
            Estudiante e = new Estudiante();
            e.setNombres("Estudiante Prueba");
            e.setApellidos("Dos");
            e.setCedula("0000000003");
            return estudianteRepository.save(e);
        });

        // 8. Crear Matrículas
        crearMatriculaSiNoExiste(est1, ano, g);
        crearMatriculaSiNoExiste(est2, ano, g);

        System.out.println("====== DATOS DE PRUEBA CREADOS ======");
        System.out.println("Usuario: " + username);
        System.out.println("ID Usuario: " + usuario.getIdUsuario());
        System.out.println("ID Persona: " + docente.getIdPersona());
        System.out.println("ID Asignacion: " + asignacion.getIdAsignacion());
        System.out.println("ID Estudiante 1: " + est1.getIdEstudiante());
        System.out.println("ID Estudiante 2: " + est2.getIdEstudiante());
        System.out.println("=====================================");
    }

    private void crearMatriculaSiNoExiste(Estudiante estudiante, AnoLectivo ano, Grado grado) {
        boolean existe = matriculaRepository.findAll()
                .stream()
                .anyMatch(m -> m.getAnoLectivo().getIdAnoLectivo().equals(ano.getIdAnoLectivo()) && 
                               m.getEstudiante().getIdEstudiante().equals(estudiante.getIdEstudiante()));
        
        if (!existe) {
            Matricula m = new Matricula();
            m.setEstudiante(estudiante);
            m.setAnoLectivo(ano);
            m.setGrado(grado);
            m.setEstado("ACTIVA");
            matriculaRepository.save(m);
        }
    }
}
