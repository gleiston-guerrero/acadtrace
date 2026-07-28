package ec.uteq.sga.soporte.controller;

import ec.uteq.sga.soporte.grpc.TecnicoGrpcClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expone al frontend la lista de tecnicos validos para asignar tickets,
 * consultando a sga-principal por gRPC (nunca por SQL cruzado ni por REST
 * directo desde el navegador).
 */
@RestController
@RequestMapping("/api/soporte")
public class UsuarioController {

    private final TecnicoGrpcClient tecnicoGrpcClient;

    public UsuarioController(TecnicoGrpcClient tecnicoGrpcClient) {
        this.tecnicoGrpcClient = tecnicoGrpcClient;
    }

    @GetMapping("/tecnicos")
    public List<Map<String, Object>> tecnicos() {
        return tecnicoGrpcClient.listarTecnicos();
    }

    /** Lista completa (todos los roles), usada por la pantalla de administración de usuarios. */
    @GetMapping("/usuarios")
    public List<Map<String, Object>> usuarios() {
        return tecnicoGrpcClient.listarPorRol(null);
    }
}