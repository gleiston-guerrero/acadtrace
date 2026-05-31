package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.CalificacionesResponse;
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

        CalificacionesResponse response = docenteGrpcClient
                .obtenerCalificaciones(idMatricula, 1L, trimestre);

        // Convertir a Map simple para evitar problemas de serialización protobuf
        Map<String, Object> resultado = new HashMap<>();
        resultado.put("promedioFormativo", response.getPromedioFormativo());
        resultado.put("promedioSumativo", response.getPromedioSumativo());
        resultado.put("promedioFinal", response.getPromedioFinal());

        List<Map<String, Object>> calificaciones = new ArrayList<>();
        for (var cal : response.getCalificacionesList()) {
            Map<String, Object> c = new HashMap<>();
            c.put("idCalificacion", cal.getIdCalificacion());


            c.put("tipoActividad", cal.getTipoActividad());
            c.put("nota", cal.getNota());
            c.put("trimestre", cal.getTrimestre());
            c.put("fecha", cal.getFecha());
            calificaciones.add(c);
        }
        resultado.put("calificaciones", calificaciones);

        return ResponseEntity.ok(resultado);
    }
}