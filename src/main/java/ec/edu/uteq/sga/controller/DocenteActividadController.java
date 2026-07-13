package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.ActividadGrpcClient;
import ec.edu.uteq.sga.grpc.actividades.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docente/actividades")
@CrossOrigin("*")
public class DocenteActividadController {

    @Autowired
    private ActividadGrpcClient actividadGrpcClient;

    @GetMapping
    public ResponseEntity<List<ActividadDto>> listarActividades(
            @RequestParam Long idAsignacion,
            @RequestParam(required = false) Long idPeriodo) {
        
        List<ActividadDto> actividades = actividadGrpcClient.listarActividades(idAsignacion, idPeriodo);
        return ResponseEntity.ok(actividades);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActividadResponse> obtenerActividad(@PathVariable Long id) {
        ActividadResponse response = actividadGrpcClient.obtenerActividad(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<ActividadResponse> crearActividad(@RequestBody CrearActividadRequest request) {
        ActividadResponse response = actividadGrpcClient.crearActividad(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ActividadResponse> editarActividad(@PathVariable Long id, @RequestBody EditarActividadRequest request) {
        EditarActividadRequest req = request.toBuilder().setIdActividad(id).build();
        ActividadResponse response = actividadGrpcClient.editarActividad(req);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<EliminarActividadResponse> eliminarActividad(@PathVariable Long id) {
        EliminarActividadResponse response = actividadGrpcClient.eliminarActividad(id);
        return ResponseEntity.ok(response);
    }
}
