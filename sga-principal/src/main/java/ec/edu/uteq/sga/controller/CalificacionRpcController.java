package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.DocenteGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CalificacionRpcController
 * -------------------------
 * Recibe peticiones HTTP del frontend React y las convierte en
 * llamadas gRPC a MICRO-DOCENTE.
 *
 * Flujo completo:
 * React → HTTP → CalificacionRpcController → gRPC → MICRO-DOCENTE → BD
 */
@RestController
@RequestMapping("/api/rpc/calificaciones")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CalificacionRpcController {

    private final DocenteGrpcClient docenteGrpcClient;

    @GetMapping("/{idMatricula}/{trimestre}")
    public ResponseEntity<List<Map<String, Object>>> obtenerCalificaciones(
            @PathVariable Long idMatricula,
            @PathVariable Integer trimestre) {
        return ResponseEntity.ok(docenteGrpcClient.obtenerCalificaciones(idMatricula, trimestre));
    }

    @GetMapping("/promedio-formativo/{idMatricula}/{trimestre}")
    public ResponseEntity<Double> promedioFormativo(
            @PathVariable Long idMatricula,
            @PathVariable Integer trimestre) {
        return ResponseEntity.ok(docenteGrpcClient.calcularPromedioFormativo(idMatricula, trimestre));
    }

    @GetMapping("/promedio-final/{idMatricula}/{trimestre}")
    public ResponseEntity<Double> promedioFinal(
            @PathVariable Long idMatricula,
            @PathVariable Integer trimestre) {
        return ResponseEntity.ok(docenteGrpcClient.calcularPromedioFinal(idMatricula, trimestre));
    }

    @PostMapping("/registrar")
    public ResponseEntity<Map<String, Object>> registrarCalificacion(
            @RequestBody Map<String, Object> body) {

        Long idMatricula  = Long.valueOf(body.get("idMatricula").toString());
        Long idActividad  = Long.valueOf(body.get("idActividad").toString());
        Double nota       = Double.valueOf(body.get("nota").toString());
        Integer trimestre = Integer.valueOf(body.get("trimestre").toString());

        return ResponseEntity.ok(
                docenteGrpcClient.registrarCalificacion(idMatricula, idActividad, nota, trimestre));
    }

    @GetMapping("/cualitativa/{nota}")
    public ResponseEntity<String> convertirACualitativa(@PathVariable Double nota) {
        return ResponseEntity.ok(docenteGrpcClient.convertirACualitativa(nota));
    }
}
