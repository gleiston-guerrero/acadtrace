package ec.edu.uteq.sga.presentation.controller;

import ec.edu.uteq.sga.domain.entity.Asignacion;
import ec.edu.uteq.sga.domain.entity.Horario;
import ec.edu.uteq.sga.domain.entity.PeriodoHorario;
import ec.edu.uteq.sga.infrastructure.repository.AsignacionRepository;
import ec.edu.uteq.sga.infrastructure.repository.HorarioRepository;
import ec.edu.uteq.sga.infrastructure.repository.PeriodoHorarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Modulo de horarios. La logica de choque (docente ocupado, curso ocupado,
 * duplicado, exceso de horas semanales) se garantiza en el trigger
 * sga_principal.fn_horario_no_choque de la BD -- ver V7__horarios.sql.
 * Aqui simplemente traducimos las excepciones a mensajes legibles.
 */
@RestController
@RequestMapping("/api/horarios")
@CrossOrigin("*")
public class HorarioController {

    private final HorarioRepository horarioRepo;
    private final PeriodoHorarioRepository periodoRepo;
    private final AsignacionRepository asignacionRepo;

    public HorarioController(HorarioRepository h, PeriodoHorarioRepository p, AsignacionRepository a) {
        this.horarioRepo = h;
        this.periodoRepo = p;
        this.asignacionRepo = a;
    }

    // ── Franjas horarias ──────────────────────────────────────
    @GetMapping("/periodos")
    public List<PeriodoHorario> listarPeriodos() {
        return periodoRepo.findByActivoTrueOrderByOrdenAsc();
    }

    // ── Grilla por CURSO (grado + paralelo + anoLectivo de una asignacion) ──
    @GetMapping("/curso/{idAsignacion}/grilla")
    @Transactional(readOnly = true)
    public ResponseEntity<?> grillaPorCurso(@PathVariable Long idAsignacion) {
        Asignacion base = asignacionRepo.findById(idAsignacion).orElse(null);
        if (base == null) return ResponseEntity.notFound().build();

        List<Asignacion> asignaciones = asignacionRepo.findAll().stream()
            .filter(a -> a.getGrado().getIdGrado().equals(base.getGrado().getIdGrado())
                      && a.getParalelo().getIdParalelo().equals(base.getParalelo().getIdParalelo())
                      && a.getAnoLectivo().getIdAnoLectivo().equals(base.getAnoLectivo().getIdAnoLectivo())
                      && a.isActivo())
            .collect(Collectors.toList());

        List<Map<String, Object>> slots = asignaciones.stream()
            .flatMap(a -> horarioRepo.findByAsignacion_IdAsignacion(a.getIdAsignacion()).stream()
                .map(h -> mapSlot(h, a)))
            .collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("curso", Map.of(
            "grado",     base.getGrado().getNombre(),
            "paralelo",  base.getParalelo().getLetra(),
            "anoLectivo", base.getAnoLectivo().getNombre()
        ));
        resp.put("periodos", listarPeriodos());
        resp.put("slots", slots);
        return ResponseEntity.ok(resp);
    }

    // ── Grilla por DOCENTE ────────────────────────────────────
    @GetMapping("/docente/{idPersona}/grilla")
    @Transactional(readOnly = true)
    public ResponseEntity<?> grillaPorDocente(@PathVariable Long idPersona) {
        List<Horario> horarios = horarioRepo.findByAsignacion_Docente_IdPersona(idPersona);
        List<Map<String, Object>> slots = horarios.stream()
            .map(h -> mapSlot(h, h.getAsignacion())).collect(Collectors.toList());

        Map<String, Object> resp = new HashMap<>();
        resp.put("periodos", listarPeriodos());
        resp.put("slots", slots);
        resp.put("totalHoras", slots.size());
        return ResponseEntity.ok(resp);
    }

    // ── Asignar slot (crea horario y valida choque via trigger) ────
    @PostMapping
    @Transactional
    public ResponseEntity<?> crearSlot(@RequestBody Map<String, Object> body) {
        try {
            Long idAsignacion = Long.valueOf(body.get("idAsignacion").toString());
            Short dia = Short.valueOf(body.get("dia").toString());
            Integer idPeriodo = Integer.valueOf(body.get("idPeriodo").toString());

            PeriodoHorario p = periodoRepo.findById(idPeriodo).orElse(null);
            Asignacion a = asignacionRepo.findById(idAsignacion).orElse(null);
            if (p == null || a == null) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "Periodo o asignacion no encontrada."));
            }

            Horario h = Horario.builder()
                .asignacion(a)
                .diaSemana(dia)
                .horaInicio(p.getHoraInicio())
                .horaFin(p.getHoraFin())
                .idPeriodo(idPeriodo)
                .aula(body.get("aula") != null ? body.get("aula").toString() : null)
                .build();

            Horario saved = horarioRepo.save(h);
            return ResponseEntity.ok(Map.of(
                "idHorario", saved.getIdHorario(),
                "mensaje", "Slot asignado."
            ));
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", e.getMessage()));
        }
    }

    // ── Eliminar slot ─────────────────────────────────────────
    @DeleteMapping("/{idHorario}")
    @Transactional
    public ResponseEntity<?> eliminarSlot(@PathVariable Long idHorario) {
        if (!horarioRepo.existsById(idHorario)) return ResponseEntity.notFound().build();
        horarioRepo.deleteById(idHorario);
        return ResponseEntity.ok(Map.of("mensaje", "Slot eliminado."));
    }

    private Map<String, Object> mapSlot(Horario h, Asignacion a) {
        Map<String, Object> m = new HashMap<>();
        m.put("idHorario",    h.getIdHorario());
        m.put("idAsignacion", a.getIdAsignacion());
        m.put("diaSemana",    h.getDiaSemana());
        m.put("idPeriodo",    h.getIdPeriodo());
        m.put("horaInicio",   h.getHoraInicio().toString());
        m.put("horaFin",      h.getHoraFin().toString());
        m.put("aula",         h.getAula());
        m.put("asignatura",   a.getAsignatura().getNombre());
        m.put("docente",      a.getDocente().getNombres() + " " + a.getDocente().getApellidos());
        m.put("grado",        a.getGrado().getNombre());
        m.put("paralelo",     a.getParalelo().getLetra());
        return m;
    }
}
