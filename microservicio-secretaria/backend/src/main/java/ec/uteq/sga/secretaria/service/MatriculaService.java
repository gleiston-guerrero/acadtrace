package ec.uteq.sga.secretaria.service;

import ec.edu.uteq.sga.grpc.principal.GuardarMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.ListarMatriculasRequest;
import ec.edu.uteq.sga.grpc.principal.MatriculaProto;
import ec.uteq.sga.secretaria.common.PageResult;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.MatriculaRequest;
import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Matricula vive en sga_secretaria (entidad JPA de sga-principal); se
 * gestiona por completo via gRPC, igual que Estudiante. historial_promocion
 * sigue siendo tabla compartida en sga_principal (fuera de alcance de esta
 * migracion, ver HistorialService), se lee por SQL directo solo para
 * enriquecer listarPorEstudiante. Grado/Paralelo/AnoLectivo se leen via
 * CatalogoService (gRPC, catalogo institucional de sga-principal).
 */
@Service
public class MatriculaService {

    private final NamedParameterJdbcTemplate jdbc;
    private final CatalogoService catalogo;
    private final PrincipalGrpcClient client;

    public MatriculaService(NamedParameterJdbcTemplate jdbc, CatalogoService catalogo, PrincipalGrpcClient client) {
        this.jdbc = jdbc;
        this.catalogo = catalogo;
        this.client = client;
    }

    public List<Map<String, Object>> paralelosPorGrado(long idGrado) {
        return catalogo.paralelos(idGrado).stream()
                .filter(CatalogoService.Paralelo::activo)
                .sorted(Comparator.comparing(CatalogoService.Paralelo::letra))
                .map(p -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id_paralelo", p.id());
                    row.put("letra", p.letra());
                    return row;
                })
                .toList();
    }

    public PageResult<Map<String, Object>> listarPorAnoLectivo(long idAnoLectivo, int page, int limit, String search) {
        ListarMatriculasRequest request = ListarMatriculasRequest.newBuilder()
                .setIdAnoLectivo(idAnoLectivo)
                .setQ(search != null ? search : "")
                .setPage(page)
                .setLimit(limit)
                .build();
        var response = client.listarMatriculas(request);
        List<Map<String, Object>> data = new ArrayList<>(response.getMatriculasList().stream().map(this::fromProto).toList());
        enriquecerConCatalogo(data);
        return PageResult.of(data, response.getTotal(), page, limit);
    }

    public List<Map<String, Object>> estadisticasPorGrado(long idAnoLectivo) {
        ListarMatriculasRequest request = ListarMatriculasRequest.newBuilder()
                .setIdAnoLectivo(idAnoLectivo)
                .setPage(1)
                .setLimit(5000)
                .build();
        List<MatriculaProto> matriculas = client.listarMatriculas(request).getMatriculasList();

        Map<String, long[]> conteoPorClave = new LinkedHashMap<>(); // {total, activas, retiradas}
        for (MatriculaProto m : matriculas) {
            String clave = m.getIdGrado() + ":" + m.getIdParalelo();
            long[] c = conteoPorClave.computeIfAbsent(clave, k -> new long[3]);
            c[0]++;
            if ("ACTIVA".equals(m.getEstado())) c[1]++;
            if ("RETIRADA".equals(m.getEstado())) c[2]++;
        }

        List<CatalogoService.Grado> grados = catalogo.grados().stream()
                .filter(CatalogoService.Grado::activo)
                .sorted(Comparator.comparingInt(CatalogoService.Grado::orden))
                .toList();

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (CatalogoService.Grado grado : grados) {
            List<CatalogoService.Paralelo> paralelos = catalogo.paralelos(grado.id()).stream()
                    .filter(CatalogoService.Paralelo::activo)
                    .sorted(Comparator.comparing(CatalogoService.Paralelo::letra))
                    .toList();
            for (CatalogoService.Paralelo paralelo : paralelos) {
                long[] c = conteoPorClave.get(grado.id() + ":" + paralelo.id());
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("grado", grado.nombre());
                row.put("paralelo", paralelo.letra());
                row.put("total", c != null ? c[0] : 0L);
                row.put("activas", c != null ? c[1] : 0L);
                row.put("retiradas", c != null ? c[2] : 0L);
                resultado.add(row);
            }
        }
        return resultado;
    }

    public List<Map<String, Object>> listarPorEstudiante(long idEstudiante) {
        ListarMatriculasRequest request = ListarMatriculasRequest.newBuilder()
                .setIdEstudiante(idEstudiante)
                .build();
        List<MatriculaProto> matriculas = client.listarMatriculas(request).getMatriculasList();
        List<Map<String, Object>> data = new ArrayList<>(matriculas.stream().map(this::fromProto).toList());
        enriquecerConCatalogo(data);
        enriquecerConHistorial(data);
        data.sort(Comparator.comparing(
                (Map<String, Object> row) -> (LocalDate) row.get("ano_lectivo_fecha_inicio"),
                Comparator.nullsLast(Comparator.reverseOrder())));
        return data;
    }

    public Map<String, Object> obtenerPorId(long id) {
        List<Map<String, Object>> filas = new ArrayList<>(List.of(fromProto(client.obtenerMatricula(id))));
        enriquecerConCatalogo(filas);
        return filas.get(0);
    }

    public Map<String, Object> crear(MatriculaRequest dto, String username) {
        Long idUsuario = resolverIdUsuario(username);
        GuardarMatriculaRequest request = GuardarMatriculaRequest.newBuilder()
                .setIdEstudiante(dto.id_estudiante())
                .setIdGrado(dto.id_grado())
                .setIdParalelo(dto.id_paralelo() != null ? dto.id_paralelo() : 0)
                .setIdAnoLectivo(dto.id_ano_lectivo())
                .setEstado(dto.estado() != null ? dto.estado() : "")
                .setObservaciones(dto.observaciones() != null ? dto.observaciones() : "")
                .setIdUsuarioRegistro(idUsuario != null ? idUsuario : 0)
                .build();
        List<Map<String, Object>> filas = new ArrayList<>(List.of(fromProto(client.crearMatricula(request))));
        enriquecerConCatalogo(filas);
        return filas.get(0);
    }

    public void cambiarEstado(long id, String estado) {
        client.cambiarEstadoMatricula(id, estado);
    }

    private Long resolverIdUsuario(String username) {
        List<Long> ids = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username",
                new MapSqlParameterSource("username", username), (rs, n) -> rs.getLong("id_usuario"));
        return ids.isEmpty() ? null : ids.get(0);
    }

    /** historial_promocion no se movio de esquema (fuera de alcance, ver HistorialService); se lee directo. */
    private void enriquecerConHistorial(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return;
        List<Long> ids = filas.stream().map(f -> (Long) f.get("id_matricula")).toList();
        List<Map<String, Object>> historial = jdbc.query(
                "SELECT id_matricula, resultado AS resultado_promocion, promedio_anual FROM sga_principal.historial_promocion WHERE id_matricula IN (:ids)",
                new MapSqlParameterSource("ids", ids), GenericRowMapper.INSTANCE);
        Map<Long, Map<String, Object>> porMatricula = new LinkedHashMap<>();
        historial.forEach(h -> porMatricula.put((Long) h.get("id_matricula"), h));
        for (Map<String, Object> fila : filas) {
            Map<String, Object> h = porMatricula.get((Long) fila.get("id_matricula"));
            fila.put("resultado_promocion", h != null ? h.get("resultado_promocion") : null);
            fila.put("promedio_anual", h != null ? h.get("promedio_anual") : null);
        }
    }

    private Map<String, Object> fromProto(MatriculaProto m) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_matricula", m.getIdMatricula());
        row.put("id_estudiante", m.getIdEstudiante());
        row.put("cedula", m.getEstudianteCedula());
        row.put("codigo_estudiante", m.getEstudianteCodigo());
        row.put("estudiante", (m.getEstudianteNombres() + " " + m.getEstudianteApellidos()).trim());
        row.put("id_grado", m.getIdGrado());
        row.put("id_paralelo", m.getIdParalelo() > 0 ? m.getIdParalelo() : null);
        row.put("id_ano_lectivo", m.getIdAnoLectivo());
        row.put("numero_orden", m.getNumeroOrden());
        row.put("fecha_registro", m.getFechaRegistro().isBlank() ? null : LocalDate.parse(m.getFechaRegistro()));
        row.put("estado", m.getEstado());
        row.put("observaciones", m.getObservaciones().isBlank() ? null : m.getObservaciones());
        row.put("registrado_por", m.getRegistradoPor().isBlank() ? null : m.getRegistradoPor());
        return row;
    }

    /**
     * Agrega grado/paralelo/ano_lectivo (nombre + letra) a cada fila usando el
     * catalogo de sga-principal por gRPC en vez de un JOIN SQL directo, para
     * no acoplar Secretaria a las tablas de catalogo de otro servicio.
     */
    private void enriquecerConCatalogo(List<Map<String, Object>> filas) {
        if (filas.isEmpty()) return;
        Map<Long, CatalogoService.Grado> grados = new LinkedHashMap<>();
        catalogo.grados().forEach(g -> grados.put(g.id(), g));
        Map<Long, CatalogoService.Paralelo> paralelos = new LinkedHashMap<>();
        catalogo.paralelos(null).forEach(p -> paralelos.put(p.id(), p));
        Map<Long, CatalogoService.AnoLectivo> anos = new LinkedHashMap<>();
        catalogo.anosLectivos().forEach(a -> anos.put(a.id(), a));

        for (Map<String, Object> fila : filas) {
            Long idGrado = toLong(fila.get("id_grado"));
            Long idParalelo = toLong(fila.get("id_paralelo"));
            Long idAno = toLong(fila.get("id_ano_lectivo"));

            CatalogoService.Grado grado = idGrado != null ? grados.get(idGrado) : null;
            CatalogoService.Paralelo paralelo = idParalelo != null ? paralelos.get(idParalelo) : null;
            CatalogoService.AnoLectivo ano = idAno != null ? anos.get(idAno) : null;

            fila.put("grado", grado != null ? grado.nombre() : null);
            fila.put("paralelo", paralelo != null ? paralelo.letra() : null);
            fila.put("ano_lectivo", ano != null ? ano.nombre() : null);
            fila.put("ano_lectivo_fecha_inicio", ano != null ? ano.fechaInicio() : null);
            fila.put("fecha_inicio", ano != null ? ano.fechaInicio() : null);
            fila.put("fecha_fin", ano != null ? ano.fechaFin() : null);
        }
    }

    private static Long toLong(Object value) {
        return value instanceof Number n ? n.longValue() : null;
    }
}
