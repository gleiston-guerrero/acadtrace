package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.AsistenciaGrpcClient;
import ec.edu.uteq.sga.grpc.asistencia.ActualizarAsistenciaRequest;
import ec.edu.uteq.sga.grpc.asistencia.AsistenciaItemRequest;
import ec.edu.uteq.sga.grpc.asistencia.AsistenciaListResponse;
import ec.edu.uteq.sga.grpc.asistencia.AsistenciaResponse;
import ec.edu.uteq.sga.grpc.asistencia.ConsultarAsistenciaRequest;
import ec.edu.uteq.sga.grpc.asistencia.ConsultarResumenRequest;
import ec.edu.uteq.sga.grpc.asistencia.RegistrarAsistenciaGrupalRequest;
import ec.edu.uteq.sga.grpc.asistencia.ResumenAsistenciaListResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/docente/asistencias")
public class DocenteAsistenciaController {

    @Autowired
    private AsistenciaGrpcClient asistenciaClient;

    @PostMapping("/masivo")
    public ResponseEntity<?> registrarAsistenciaGrupal(@RequestBody Map<String, Object> body) {
        int idAsignacion = (Integer) body.get("id_asignacion");
        int idPeriodo = (Integer) body.get("id_periodo");
        String fecha = (String) body.get("fecha");
        
        List<Map<String, Object>> asistenciasRaw = (List<Map<String, Object>>) body.get("asistencias");
        
        RegistrarAsistenciaGrupalRequest.Builder requestBuilder = RegistrarAsistenciaGrupalRequest.newBuilder()
                .setIdAsignacion(idAsignacion)
                .setIdPeriodo(idPeriodo)
                .setFecha(fecha);
                
        for (Map<String, Object> asis : asistenciasRaw) {
            requestBuilder.addAsistencias(AsistenciaItemRequest.newBuilder()
                    .setIdMatricula((Integer) asis.get("id_matricula"))
                    .setEstado((String) asis.get("estado"))
                    .setJustificacion(asis.get("justificacion") != null ? (String) asis.get("justificacion") : "")
                    .build());
        }
        
        try {
            AsistenciaListResponse response = asistenciaClient.registrarAsistenciaGrupal(requestBuilder.build());
            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", response.getMessage()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", response.getMessage()));
            }
        } catch (io.grpc.StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.ALREADY_EXISTS) {
                return ResponseEntity.status(409).body(Map.of("message", e.getStatus().getDescription()));
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getStatus().getDescription()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarAsistencia(@PathVariable int id, @RequestBody Map<String, Object> body) {
        String estado = (String) body.get("estado");
        String justificacion = body.get("justificacion") != null ? (String) body.get("justificacion") : "";
        
        ActualizarAsistenciaRequest request = ActualizarAsistenciaRequest.newBuilder()
                .setIdAsistencia(id)
                .setEstado(estado)
                .setJustificacion(justificacion)
                .build();
                
        try {
            AsistenciaResponse response = asistenciaClient.actualizarAsistencia(request);
            if (response.getSuccess()) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", response.getMessage()
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", response.getMessage()));
            }
        } catch (io.grpc.StatusRuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getStatus().getDescription()));
        }
    }

    @GetMapping("/asignacion/{idAsignacion}")
    public ResponseEntity<?> consultarAsistencia(
            @PathVariable int idAsignacion,
            @RequestParam(required = false) String fecha,
            @RequestParam(required = false, defaultValue = "0") int idPeriodo,
            @RequestParam(required = false, defaultValue = "0") int idMatricula) {
            
        ConsultarAsistenciaRequest.Builder builder = ConsultarAsistenciaRequest.newBuilder()
                .setIdAsignacion(idAsignacion)
                .setIdPeriodo(idPeriodo)
                .setIdMatricula(idMatricula);
                
        if (fecha != null && !fecha.isEmpty()) {
            builder.setFecha(fecha);
        }
        
        AsistenciaListResponse response = asistenciaClient.consultarAsistencia(builder.build());
        
        List<Map<String, Object>> asistencias = response.getAsistenciasList().stream()
            .map(a -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id_asistencia", a.getIdAsistencia());
                map.put("id_matricula", a.getIdMatricula());
                map.put("id_asignacion", a.getIdAsignacion());
                map.put("id_periodo", a.getIdPeriodo());
                map.put("fecha", a.getFecha());
                map.put("estado", a.getEstado());
                map.put("justificacion", a.getJustificacion());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(Map.of(
            "success", true,
            "asistencias", asistencias
        ));
    }

    @GetMapping("/asignacion/{idAsignacion}/resumen")
    public ResponseEntity<?> consultarResumenAsistencia(
            @PathVariable int idAsignacion,
            @RequestParam(required = false, defaultValue = "0") int idPeriodo,
            @RequestParam(required = false, defaultValue = "0") int idMatricula) {
            
        ConsultarResumenRequest request = ConsultarResumenRequest.newBuilder()
                .setIdAsignacion(idAsignacion)
                .setIdPeriodo(idPeriodo)
                .setIdMatricula(idMatricula)
                .build();
                
        ResumenAsistenciaListResponse response = asistenciaClient.consultarResumenAsistencia(request);
        
        List<Map<String, Object>> resumenes = response.getResumenesList().stream()
            .map(r -> {
                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id_resumen", r.getIdResumen());
                map.put("id_matricula", r.getIdMatricula());
                map.put("id_asignacion", r.getIdAsignacion());
                map.put("id_periodo", r.getIdPeriodo());
                map.put("total_presentes", r.getTotalPresentes());
                map.put("total_ausentes", r.getTotalAusentes());
                map.put("total_justificados", r.getTotalJustificados());
                map.put("total_atrasos", r.getTotalAtrasos());
                map.put("porcentaje_asistencia", r.getPorcentajeAsistencia());
                return map;
            })
            .collect(Collectors.toList());
            
        return ResponseEntity.ok(Map.of(
            "success", true,
            "resumenes", resumenes
        ));
    }
}
