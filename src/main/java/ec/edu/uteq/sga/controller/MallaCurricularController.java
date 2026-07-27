package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignatura;
import ec.edu.uteq.sga.entity.Grado;
import ec.edu.uteq.sga.entity.MallaCurricular;
import ec.edu.uteq.sga.repository.AnoLectivoRepository;
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

    @GetMapping("/grado/{idGrado}")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Object>> porGrado(@PathVariable Long idGrado,
                                                        @RequestParam Long idAnoLectivo) {
        List<MallaCurricular> malla = mallaRepo.findByGradoAnoWithAsignatura(idGrado, idAnoLectivo);
        int total = malla.stream().mapToInt(m -> m.getHorasSemana() != null ? m.getHorasSemana() : 0).sum();
        List<Map<String, Object>> items = malla.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("idMalla", m.getIdMalla());
            map.put("idAsignatura", m.getAsignatura().getIdAsignatura());
            map.put("asignatura", m.getAsignatura().getNombre());
            map.put("codigo", m.getAsignatura().getCodigo());
            map.put("horasSemana", m.getHorasSemana());
            map.put("diasSemana", m.getDiasSemana());
            map.put("duracion", m.getDuracion());
            return map;
        }).collect(Collectors.toList());
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
