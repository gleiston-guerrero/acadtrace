package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.entity.Matricula;
import ec.edu.uteq.sga.repository.MatriculaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Consulta de solo lectura de estudiantes matriculados por grado/paralelo.
 * La escritura de estudiantes vive en Secretaría; aquí solo se lee de la BD
 * compartida para el Director (módulos Grados y vista de cursos).
 */
@RestController
@RequestMapping("/api/estudiantes")
@RequiredArgsConstructor
public class EstudianteConsultaController {

    private final MatriculaRepository matriculaRepository;

    @GetMapping("/por-grado")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> porGrado(
            @RequestParam Long idGrado,
            @RequestParam Long idAnoLectivo,
            @RequestParam(required = false) Long idParalelo) {

        List<Matricula> matriculas = (idParalelo != null)
                ? matriculaRepository.findByGradoParaleloAndAnoLectivoWithEstudiante(idGrado, idParalelo, idAnoLectivo)
                : matriculaRepository.findByGradoAndAnoLectivoWithEstudiante(idGrado, idAnoLectivo);

        List<Map<String, Object>> resp = matriculas.stream().map(m -> {
            var e = m.getEstudiante();
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("idMatricula", m.getIdMatricula());
            map.put("idEstudiante", e.getIdEstudiante());
            map.put("cedula", e.getCedula());
            map.put("codigoEstudiante", e.getCodigoEstudiante());
            map.put("nombres", e.getNombres());
            map.put("apellidos", e.getApellidos());
            map.put("genero", e.getGenero());
            map.put("estado", m.getEstado());
            map.put("numeroOrden", m.getNumeroOrden());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(resp);
    }
}
