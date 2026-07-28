package ec.uteq.sga.secretaria.service;

import ec.edu.uteq.sga.grpc.principal.GradoProto;
import ec.edu.uteq.sga.grpc.principal.GuardarGradoRequest;
import ec.edu.uteq.sga.grpc.principal.ParaleloProto;
import ec.uteq.sga.secretaria.dto.GradoRequest;
import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Gestion de Grado/Paralelo desde Secretaria, via gRPC hacia sga-principal.
 * Grado/Paralelo siguen siendo catalogo institucional propiedad de
 * sga-principal (a diferencia de Estudiante, que se movio a sga_secretaria):
 * este servicio no tiene su propia copia de los datos, solo expone el CRUD
 * ya existente en sga-principal (GradoService) a traves de gRPC, para que
 * Secretaria pueda gestionarlo desde su propio frontend. CatalogoService
 * sigue siendo el punto de lectura de solo-catalogo para el resto de la app
 * (Matriculas, Reportes, Historial); este servicio es el de gestion.
 */
@Service
public class GradoService {

    private final PrincipalGrpcClient client;

    public GradoService(PrincipalGrpcClient client) {
        this.client = client;
    }

    public List<Map<String, Object>> listarTodos() {
        List<ParaleloProto> paralelos = client.listarParalelos(null);
        Map<Long, List<Map<String, Object>>> paralelosPorGrado = paralelos.stream()
                .map(this::fromProto)
                .collect(Collectors.groupingBy(p -> (Long) p.get("id_grado"), LinkedHashMap::new, Collectors.toList()));

        return client.listarGrados().stream()
                .map(g -> {
                    Map<String, Object> row = fromProto(g);
                    row.put("paralelos", paralelosPorGrado.getOrDefault(g.getIdGrado(), new ArrayList<>()));
                    return row;
                })
                .toList();
    }

    public List<Map<String, Object>> listarParalelos(Long idGrado) {
        return client.listarParalelos(idGrado).stream().map(this::fromProto).toList();
    }

    public Map<String, Object> crear(GradoRequest dto) {
        return fromProto(client.crearGrado(toGuardarRequest(0, dto)));
    }

    public Map<String, Object> actualizar(long idGrado, GradoRequest dto) {
        return fromProto(client.actualizarGrado(toGuardarRequest(idGrado, dto)));
    }

    public void cambiarEstado(long idGrado, boolean activo) {
        client.cambiarEstadoGrado(idGrado, activo);
    }

    public Map<String, Object> crearParalelo(long idGrado, String letra) {
        return fromProto(client.crearParalelo(idGrado, letra));
    }

    public void cambiarEstadoParalelo(long idParalelo, boolean activo) {
        client.cambiarEstadoParalelo(idParalelo, activo);
    }

    private GuardarGradoRequest toGuardarRequest(long idGrado, GradoRequest dto) {
        return GuardarGradoRequest.newBuilder()
                .setIdGrado(idGrado)
                .setNombre(dto.nombre())
                .setOrden(dto.orden() != null ? dto.orden() : 0)
                .setCapacidadMax(dto.capacidad_max() != null ? dto.capacidad_max() : 0)
                .setIdNivel(dto.id_nivel() != null ? dto.id_nivel() : 0)
                .build();
    }

    private Map<String, Object> fromProto(GradoProto g) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_grado", g.getIdGrado());
        row.put("nombre", g.getNombre());
        row.put("orden", g.getOrden());
        row.put("activo", g.getActivo());
        row.put("capacidad_max", g.getCapacidadMax());
        row.put("id_nivel", g.getIdNivel() > 0 ? g.getIdNivel() : null);
        row.put("nivel_educativo", g.getNivelEducativo().isBlank() ? null : g.getNivelEducativo());
        row.put("tipo_escala", g.getTipoEscala().isBlank() ? null : g.getTipoEscala());
        return row;
    }

    private Map<String, Object> fromProto(ParaleloProto p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_paralelo", p.getIdParalelo());
        row.put("id_grado", p.getIdGrado());
        row.put("letra", p.getLetra());
        row.put("activo", p.getActivo());
        row.put("total_estudiantes", p.getTotalEstudiantes());
        return row;
    }
}
