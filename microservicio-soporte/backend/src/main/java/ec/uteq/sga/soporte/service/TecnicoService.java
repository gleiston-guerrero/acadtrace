package ec.uteq.sga.soporte.service;

import ec.uteq.sga.soporte.common.ApiException;
import ec.uteq.sga.soporte.grpc.TecnicoGrpcClient;
import io.grpc.StatusRuntimeException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Usuarios validos para asignar/escalar tickets (SOPORTE_TECNICO o
 * DIRECTOR). Consulta a sga-principal por gRPC via TecnicoGrpcClient, nunca
 * por SQL cruzado: soporte sigue siendo dueno unicamente de su propio
 * esquema (tickets, comentarios, historial_ticket).
 */
@Service
public class TecnicoService {

    private static final Set<String> ROLES_TECNICOS = Set.of("SOPORTE_TECNICO", "DIRECTOR");

    private final TecnicoGrpcClient grpcClient;

    public TecnicoService(TecnicoGrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    /**
     * Trae todos los usuarios activos y filtra aqui por rol tecnico/director
     * (dos roles a la vez), en lugar de llamar dos veces al gRPC.
     */
    public List<Map<String, Object>> listarTecnicos() {
        List<Map<String, Object>> usuarios;
        try {
            usuarios = grpcClient.listarPorRol("");
        } catch (StatusRuntimeException e) {
            throw ApiException.badRequest(
                    "No se pudo consultar los técnicos en sga-principal (gRPC): " + e.getStatus().getDescription());
        }

        return usuarios.stream()
                .filter(u -> Boolean.TRUE.equals(u.get("activo")))
                .filter(u -> {
                    Object rolesObj = u.get("roles");
                    if (!(rolesObj instanceof List<?> roles)) {
                        return false;
                    }
                    return roles.stream().anyMatch(r -> ROLES_TECNICOS.contains(String.valueOf(r)));
                })
                .toList();
    }
}
