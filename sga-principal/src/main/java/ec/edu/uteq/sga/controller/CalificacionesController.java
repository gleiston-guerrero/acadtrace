package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.DocenteGrpcClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/calificaciones")
@RequiredArgsConstructor
public class CalificacionesController {

    private final DocenteGrpcClient docenteGrpcClient;

    @GetMapping("/matricula/{idMatricula}/trimestre/{trimestre}")
    public ResponseEntity<?> obtenerCalificaciones(
            @PathVariable Long idMatricula,
            @PathVariable Integer trimestre) {

        List<Map<String, Object>> calificaciones = docenteGrpcClient.obtenerCalificaciones(idMatricula, trimestre);
        Double promedioFormativo = docenteGrpcClient.calcularPromedioFormativo(idMatricula, trimestre);
        Double promedioFinal     = docenteGrpcClient.calcularPromedioFinal(idMatricula, trimestre);

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("promedioFormativo", promedioFormativo);
        resultado.put("promedioSumativo",  0.0);
        resultado.put("promedioFinal",     promedioFinal);
        resultado.put("calificaciones",    calificaciones);

        return ResponseEntity.ok(resultado);
    }
}