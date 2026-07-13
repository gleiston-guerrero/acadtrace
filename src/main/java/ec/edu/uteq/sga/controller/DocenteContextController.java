package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.entity.AnoLectivo;
import ec.edu.uteq.sga.entity.Asignacion;
import ec.edu.uteq.sga.entity.Matricula;
import ec.edu.uteq.sga.entity.Persona;
import ec.edu.uteq.sga.service.TeacherAuthorizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/docentes")
@CrossOrigin("*")
public class DocenteContextController {

    @Autowired
    private TeacherAuthorizationService authService;

    @GetMapping("/mis-asignaciones")
    public ResponseEntity<List<Map<String, Object>>> getMisAsignaciones() {
        Persona docente = authService.getAuthenticatedTeacher();
        List<Asignacion> asignaciones = authService.getTeacherAssignments(docente.getIdPersona());
        
        List<Map<String, Object>> response = asignaciones.stream()
            .filter(Asignacion::isActivo)
            .map(a -> {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("idAsignacion", a.getIdAsignacion());
                map.put("asignatura", Map.of(
                    "id", a.getAsignatura().getIdAsignatura(),
                    "nombre", a.getAsignatura().getNombre()
                ));
                map.put("grado", Map.of(
                    "id", a.getGrado().getIdGrado(),
                    "nombre", a.getGrado().getNombre()
                ));
                map.put("anoLectivo", Map.of(
                    "id", a.getAnoLectivo().getIdAnoLectivo(),
                    "nombre", a.getAnoLectivo().getNombre()
                ));
                return map;
            }).collect(Collectors.toList());
            
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asignaciones/{id}/estudiantes")
    public ResponseEntity<List<Map<String, Object>>> getEstudiantesAsignacion(@PathVariable Long id) {
        Persona docente = authService.getAuthenticatedTeacher();
        Asignacion asignacion = authService.validateTeacherAssignment(docente.getIdPersona(), id);
        
        List<Matricula> matriculas = authService.getStudentsByAssignment(id);
        
        List<Map<String, Object>> response = matriculas.stream().map(m -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("idMatricula", m.getIdMatricula());
            map.put("estudiante", Map.of(
                "id", m.getEstudiante().getIdEstudiante(),
                "nombres", m.getEstudiante().getNombres(),
                "apellidos", m.getEstudiante().getApellidos(),
                "cedula", m.getEstudiante().getCedula()
            ));
            map.put("estado", m.getEstado());
            return map;
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/ano-actual")
    public ResponseEntity<Map<String, Object>> getAnoActual() {
        AnoLectivo ano = authService.getCurrentAcademicYear();
        return ResponseEntity.ok(Map.of(
            "idAnoLectivo", ano.getIdAnoLectivo(),
            "nombre", ano.getNombre()
        ));
    }
}
