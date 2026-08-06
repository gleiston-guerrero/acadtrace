package ec.uteq.sga.secretaria.controller;

import ec.edu.uteq.sga.grpc.principal.RepresentanteProto;
import ec.uteq.sga.secretaria.dto.RepresentanteRequest;
import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import ec.uteq.sga.secretaria.service.RepresentanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/secretario/representantes")
public class RepresentanteController {

    private final RepresentanteService service;
    private final PrincipalGrpcClient principalGrpc;

    public RepresentanteController(RepresentanteService service, PrincipalGrpcClient principalGrpc) {
        this.service = service;
        this.principalGrpc = principalGrpc;
    }

    /**
     * Consulta el representante desde sga-principal mediante gRPC (mirror en
     * sga_principal.representantes). Demuestra el consumo cross-microservicio
     * del contrato PrincipalService.ObtenerRepresentante.
     */
    @GetMapping("/grpc/{id}")
    public Map<String, Object> obtenerViaGrpc(@PathVariable Long id) {
        RepresentanteProto r = principalGrpc.obtenerRepresentante(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("idRepresentante", r.getIdRepresentante());
        resp.put("cedula", r.getCedula());
        resp.put("nombres", r.getNombres());
        resp.put("apellidos", r.getApellidos());
        resp.put("parentesco", r.getParentesco());
        resp.put("telefonoPrincipal", r.getTelefonoPrincipal());
        resp.put("telefonoAlt", r.getTelefonoAlt());
        resp.put("correo", r.getCorreo());
        resp.put("direccion", r.getDireccion());
        resp.put("fuente", "gRPC → sga-principal");
        return resp;
    }

    @GetMapping
    public List<Map<String, Object>> listar(@RequestParam(name = "q", required = false) String search) {
        return service.listarTodos(search);
    }

    @GetMapping("/{id}")
    public Map<String, Object> obtener(@PathVariable Long id) {
        return service.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> crear(@Valid @RequestBody RepresentanteRequest dto) {
        return service.crear(dto);
    }

    @PutMapping("/{id}")
    public Map<String, Object> actualizar(@PathVariable Long id, @Valid @RequestBody RepresentanteRequest dto) {
        return service.actualizar(id, dto);
    }
}
