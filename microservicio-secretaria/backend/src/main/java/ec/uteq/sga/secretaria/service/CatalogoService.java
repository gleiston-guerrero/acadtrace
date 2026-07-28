package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * Envuelve PrincipalGrpcClient y traduce los proto (AnoLectivoProto, GradoProto,
 * ParaleloProto, AsignaturaProto) a records simples, para que EstudianteService/
 * MatriculaService/HistorialService/ReportesService no dependan directamente de
 * las clases generadas por protobuf. Reemplaza los antiguos
 * "JOIN sga_principal.grados/paralelos/anos_lectivos": estas tablas son
 * catalogo institucional, no dominio de Secretaria, y se leen por gRPC.
 */
@Service
public class CatalogoService {

    private final PrincipalGrpcClient client;

    public CatalogoService(PrincipalGrpcClient client) {
        this.client = client;
    }

    public record Grado(long id, String nombre, int orden, boolean activo) {}

    public record Paralelo(long id, long idGrado, String letra, boolean activo) {}

    public record AnoLectivo(long id, String nombre, LocalDate fechaInicio, LocalDate fechaFin, boolean esActual) {}

    public record Asignatura(long id, String nombre, String codigo, boolean activa) {}

    public List<Grado> grados() {
        return client.listarGrados().stream()
                .map(g -> new Grado(g.getIdGrado(), g.getNombre(), g.getOrden(), g.getActivo()))
                .toList();
    }

    public List<Paralelo> paralelos(Long idGrado) {
        return client.listarParalelos(idGrado).stream()
                .map(p -> new Paralelo(p.getIdParalelo(), p.getIdGrado(), p.getLetra(), p.getActivo()))
                .toList();
    }

    public List<AnoLectivo> anosLectivos() {
        return client.listarAnosLectivos().stream()
                .map(a -> new AnoLectivo(a.getIdAnoLectivo(), a.getNombre(),
                        parseFecha(a.getFechaInicio()), parseFecha(a.getFechaFin()), a.getEsActual()))
                .toList();
    }

    public List<Asignatura> asignaturas() {
        return client.listarAsignaturas().stream()
                .map(a -> new Asignatura(a.getIdAsignatura(), a.getNombre(), a.getCodigo(), a.getActiva()))
                .toList();
    }

    private static LocalDate parseFecha(String value) {
        return (value == null || value.isBlank()) ? null : LocalDate.parse(value);
    }
}
