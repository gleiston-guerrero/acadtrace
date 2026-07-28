package ec.uteq.sga.soporte.controller;

import ec.uteq.sga.soporte.service.TecnicoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lista de tecnicos/directores validos para asignar o escalar tickets.
 * Los datos vienen de sga-principal por gRPC (ver TecnicoGrpcClient); este
 * endpoint sigue protegido por JwtAuthFilter como el resto de /api/soporte/*.
 */
@RestController
@RequestMapping("/api/soporte/tecnicos")
public class TecnicoController {

    private final TecnicoService service;

    public TecnicoController(TecnicoService service) {
        this.service = service;
    }

    @GetMapping
    public List<Map<String, Object>> listar() {
        return service.listarTecnicos();
    }
}
