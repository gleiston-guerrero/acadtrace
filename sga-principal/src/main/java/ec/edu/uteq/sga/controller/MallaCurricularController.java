package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignacion;
import ec.edu.uteq.sga.entity.Asignatura;
import ec.edu.uteq.sga.entity.Grado;
import ec.edu.uteq.sga.entity.MallaCurricular;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
import ec.edu.uteq.sga.repository.AsignacionRepository;
import ec.edu.uteq.sga.repository.AsignaturaRepository;
import ec.edu.uteq.sga.repository.GradoRepository;
import ec.edu.uteq.sga.repository.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/malla")
@RequiredArgsConstructor
public class MallaCurricularController {

    private final MallaCurricularRepository mallaRepo;
    private final GradoRepository gradoRepo;
    private final AsignaturaRepository asignaturaRepo;
    private final AnoLectivoRepository anoLectivoRepo;
    private final AsignacionRepository asignacionRepo;

    @GetMapping("/grado/{idGrado}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> porGrado(@PathVariable Long idGrado,
                                                        @RequestParam Long idAnoLectivo) {
        List<MallaCurricular> malla = mallaRepo.findByGradoAnoWithAsignatura(idGrado, idAnoLectivo);
        List<Asignacion> asignaciones = asignacionRepo.findAll().stream()
                .filter(a -> a.getGrado().getIdGrado().equals(idGrado) 
                        && a.getAnoLectivo().getIdAnoLectivo().equals(idAnoLectivo)
                        && a.isActivo())
                .collect(Collectors.toList());

        Map<Long, Map<String, Object>> mapaMaterias = new LinkedHashMap<>();

        for (MallaCurricular m : malla) {
            Asignatura asig = m.getAsignatura();
            Map<String, Object> item = new HashMap<>();
            item.put("idMalla", m.getIdMalla());
            item.put("idAsignatura", asig.getIdAsignatura());
            item.put("asignatura", asig.getNombre());
            item.put("codigo", asig.getCodigo());
            item.put("horasSemana", m.getHorasSemana() != null ? m.getHorasSemana() : 4);
            item.put("diasSemana", m.getDiasSemana());
            item.put("duracion", m.getDuracion());
            item.put("docentes", new ArrayList<String>());
            item.put("origen", "MALLA");
            mapaMaterias.put(asig.getIdAsignatura(), item);
        }

        for (Asignacion a : asignaciones) {
            Asignatura asig = a.getAsignatura();
            Long idAsig = asig.getIdAsignatura();
            String nombreDocente = a.getDocente().getNombres() + " " + a.getDocente().getApellidos();
            int horas = a.getHorasSemanales() != null && a.getHorasSemanales() > 0 ? a.getHorasSemanales() : 4;

            if (mapaMaterias.containsKey(idAsig)) {
                Map<String, Object> item = mapaMaterias.get(idAsig);
                @SuppressWarnings("unchecked")
                List<String> docs = (List<String>) item.get("docentes");
                if (!docs.contains(nombreDocente)) docs.add(nombreDocente);
                int hActual = Integer.parseInt(item.get("horasSemana").toString());
                if (horas > hActual) item.put("horasSemana", horas);
            } else {
                Map<String, Object> item = new HashMap<>();
                item.put("idMalla", null);
                item.put("idAsignatura", asig.getIdAsignatura());
                item.put("asignatura", asig.getNombre());
                item.put("codigo", asig.getCodigo());
                item.put("horasSemana", horas);
                List<String> docs = new ArrayList<>();
                docs.add(nombreDocente);
                item.put("docentes", docs);
                item.put("origen", "ASIGNACION");
                mapaMaterias.put(idAsig, item);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>(mapaMaterias.values());
        int total = items.stream().mapToInt(i -> Integer.parseInt(i.get("horasSemana").toString())).sum();

        return ResponseEntity.ok(Map.of("totalHoras", total, "materias", items));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> agregar(@RequestBody Map<String, Object> body) {
        Long idGrado = Long.valueOf(body.get("idGrado").toString());
        Long idAsignatura = Long.valueOf(body.get("idAsignatura").toString());
        Long idAnoLectivo = Long.valueOf(body.get("idAnoLectivo").toString());
        Short horas = Short.valueOf(body.get("horasSemana").toString());

        if (mallaRepo.existsByGrado_IdGradoAndAsignatura_IdAsignaturaAndAnoLectivo_IdAnoLectivo(idGrado, idAsignatura, idAnoLectivo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esa asignatura ya está en la malla del grado");
        }
        Grado grado = gradoRepo.findById(idGrado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Grado no existe"));
        Asignatura asignatura = asignaturaRepo.findById(idAsignatura)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignatura no existe"));
        AnoLectivo ano = anoLectivoRepo.findById(idAnoLectivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Año lectivo no existe"));

        mallaRepo.save(MallaCurricular.builder()
                .grado(grado).asignatura(asignatura).anoLectivo(ano).horasSemana(horas)
                .diasSemana(body.get("diasSemana") != null ? Short.valueOf(body.get("diasSemana").toString()) : null)
                .duracion(body.get("duracion") != null ? Short.valueOf(body.get("duracion").toString()) : null)
                .build());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> actualizarHoras(@PathVariable Long id, @RequestParam Short horasSemana) {
        MallaCurricular m = mallaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no existe"));
        m.setHorasSemana(horasSemana);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!mallaRepo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro no existe");
        }
        mallaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
