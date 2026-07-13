package ec.edu.uteq.sga.controller;

import ec.edu.uteq.sga.grpc.DocenteGrpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test-grpc")
public class TestFlowController {

    @Autowired
    private DocenteGrpcClient grpcClient;

    @GetMapping("/flow")
    public Map<String, Object> testFlow() {
        // Envia idMatricula=1, idActividad=1, nota=10.0, trimestre=1
        // DocenteGrpcClient internamente agregará el header docente_id = "1"
        return grpcClient.registrarCalificacion(1L, 1L, 10.0, 1);
    }
}
