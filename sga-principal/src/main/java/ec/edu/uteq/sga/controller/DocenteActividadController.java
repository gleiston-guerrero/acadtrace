package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.ActividadGrpcClient;
import ec.edu.uteq.sga.grpc.actividades.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/docente/actividades")
@CrossOrigin("*")
public class DocenteActividadController {

    @Autowired
    private ActividadGrpcClient actividadGrpcClient;

    @Autowired
    private ec.edu.uteq.sga.repository.PeriodoEvaluacionRepository periodoEvaluacionRepository;

    @GetMapping("/periodos")
    public ResponseEntity<?> listarPeriodos() {
        List<ec.edu.uteq.sga.entity.PeriodoEvaluacion> periodos = periodoEvaluacionRepository.findAll();
        List<Map<String, Object>> response = periodos.stream().map(p -> {
            Map<String, Object> map = new java.util.HashMap<>();
            map.put("id_periodo", p.getIdPeriodo());
            map.put("nombre", p.getNombre());
            map.put("tipo", p.getTipo());
            map.put("activo", p.isActivo());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> mapActividadDto(ActividadDto a) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("idActividad", a.getIdActividad());
        map.put("idAsignacion", a.getIdAsignacion());
        map.put("idPeriodo", a.getIdPeriodo());
        map.put("tipo", a.getTipo());
        map.put("nombre", a.getNombre());
        map.put("descripcion", a.getDescripcion());
        map.put("fechaEntrega", a.getFechaEntrega());
        map.put("ponderacion", a.getPonderacion());
        map.put("notaMaxima", a.getNotaMaxima());
        map.put("esSumativa", a.getEsSumativa());
        return map;
    }

    private Map<String, Object> mapActividadResponse(ActividadResponse r) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("exitoso", r.getExitoso());
        map.put("mensaje", r.getMensaje());
        if (r.hasActividad()) {
            map.put("actividad", mapActividadDto(r.getActividad()));
        }
        return map;
    }

    private Map<String, Object> mapEliminarResponse(EliminarActividadResponse r) {
        Map<String, Object> map = new java.util.HashMap<>();
        map.put("exitoso", r.getExitoso());
        map.put("mensaje", r.getMensaje());
        return map;
    }

    @GetMapping
    public ResponseEntity<?> listarActividades(
            @RequestParam Long idAsignacion,
            @RequestParam(required = false) Long idPeriodo) {

        List<ActividadDto> actividades = actividadGrpcClient.listarActividades(idAsignacion, idPeriodo);
        List<Map<String, Object>> response = actividades.stream()
                .map(this::mapActividadDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/asignacion/{asignacionId}")
    public ResponseEntity<List<ActividadDto>> listarActividadesPorAsignacion(
            @PathVariable Long asignacionId,
            @RequestParam(required = false) Long idPeriodo) {

        List<ActividadDto> actividades = actividadGrpcClient.listarActividades(asignacionId, idPeriodo);
        return ResponseEntity.ok(actividades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerActividad(@PathVariable Long id) {
        ActividadResponse response = actividadGrpcClient.obtenerActividad(id);
        return ResponseEntity.ok(mapActividadResponse(response));
    }

    @PostMapping
    public ResponseEntity<?> crearActividad(@RequestBody Map<String, Object> body) {
        CrearActividadRequest request = CrearActividadRequest.newBuilder()
                .setIdAsignacion(Long.valueOf(body.get("asignacionId").toString()))
                .setIdPeriodo(Long.valueOf(body.get("periodoId").toString()))
                .setTipo((String) body.get("tipo"))
                .setNombre((String) body.get("nombre"))
                .setDescripcion(body.get("descripcion") != null ? (String) body.get("descripcion") : "")
                .setFechaEntrega((String) body.get("fechaEntrega"))
                .setPonderacion(Double.valueOf(body.get("ponderacion").toString()))
                .setNotaMaxima(Double.valueOf(body.get("notaMaxima").toString()))
                .setEsSumativa((Boolean) body.get("esSumativa"))
                .build();
        ActividadResponse response = actividadGrpcClient.crearActividad(request);
        return ResponseEntity.ok(mapActividadResponse(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarActividad(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        EditarActividadRequest request = EditarActividadRequest.newBuilder()
                .setIdActividad(id)
                .setTipo((String) body.get("tipo"))
                .setNombre((String) body.get("nombre"))
                .setDescripcion(body.get("descripcion") != null ? (String) body.get("descripcion") : "")
                .setFechaEntrega((String) body.get("fechaEntrega"))
                .setPonderacion(Double.valueOf(body.get("ponderacion").toString()))
                .setNotaMaxima(Double.valueOf(body.get("notaMaxima").toString()))
                .setEsSumativa((Boolean) body.get("esSumativa"))
                .build();
        ActividadResponse response = actividadGrpcClient.editarActividad(request);
        return ResponseEntity.ok(mapActividadResponse(response));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarActividad(@PathVariable Long id) {
        EliminarActividadResponse response = actividadGrpcClient.eliminarActividad(id);
        return ResponseEntity.ok(mapEliminarResponse(response));
    }
}
