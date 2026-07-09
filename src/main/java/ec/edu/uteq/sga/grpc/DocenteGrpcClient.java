package ec.edu.uteq.sga.grpc;

import ec.edu.uteq.sga.grpc.docente.*;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente gRPC hacia MICRO-DOCENTE.
 * Reemplaza al antiguo DocenteRpcClient (XML-RPC), que solo se usó para
 * una presentación y ya no está en uso.
 */
@Component
public class DocenteGrpcClient {

    @GrpcClient("docente-service")
    private DocenteServiceGrpc.DocenteServiceBlockingStub stub;

    public List<Map<String, Object>> obtenerCalificaciones(Long idMatricula, Integer trimestre) {
        try {
            ObtenerCalificacionesRequest request = ObtenerCalificacionesRequest.newBuilder()
                    .setIdMatricula(idMatricula)
                    .setTrimestre(trimestre)
                    .build();

            ObtenerCalificacionesResponse response = stub.obtenerCalificaciones(request);

            return response.getCalificacionesList().stream()
                    .map(c -> Map.<String, Object>of(
                            "idCalificacion", c.getIdCalificacion(),
                            "idActividad", c.getIdActividad(),
                            "idMatricula", c.getIdMatricula(),
                            "nota", c.getNota(),
                            "notaCualitativa", c.getNotaCualitativa(),
                            "registradoPor", c.getRegistradoPor()
                    ))
                    .collect(Collectors.toList());
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC obtenerCalificaciones: " + e.getMessage());
            return List.of();
        }
    }

    public Double calcularPromedioFormativo(Long idMatricula, Integer trimestre) {
        try {
            PromedioRequest request = PromedioRequest.newBuilder()
                    .setIdMatricula(idMatricula)
                    .setTrimestre(trimestre)
                    .build();
            return stub.calcularPromedioFormativo(request).getPromedio();
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC calcularPromedioFormativo: " + e.getMessage());
            return 0.0;
        }
    }

    public Double calcularPromedioFinal(Long idMatricula, Integer trimestre) {
        try {
            PromedioRequest request = PromedioRequest.newBuilder()
                    .setIdMatricula(idMatricula)
                    .setTrimestre(trimestre)
                    .build();
            return stub.calcularPromedioFinal(request).getPromedio();
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC calcularPromedioFinal: " + e.getMessage());
            return 0.0;
        }
    }

    public Map<String, Object> registrarCalificacion(Long idMatricula, Long idActividad,
                                                      Double nota, Integer trimestre) {
        try {
            RegistrarCalificacionRequest request = RegistrarCalificacionRequest.newBuilder()
                    .setIdMatricula(idMatricula)
                    .setIdActividad(idActividad)
                    .setNota(nota)
                    .setTrimestre(trimestre)
                    .build();

            RegistrarCalificacionResponse response = stub.registrarCalificacion(request);

            return Map.of(
                    "exitoso", response.getExitoso(),
                    "mensaje", response.getMensaje(),
                    "idCalificacion", response.getIdCalificacion()
            );
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC registrarCalificacion: " + e.getMessage());
            return Map.of("exitoso", false, "mensaje", e.getMessage(), "idCalificacion", 0L);
        }
    }

    public String convertirACualitativa(Double nota) {
        try {
            ConvertirACualitativaRequest request = ConvertirACualitativaRequest.newBuilder()
                    .setNota(nota)
                    .build();
            return stub.convertirACualitativa(request).getCualitativa();
        } catch (StatusRuntimeException e) {
            System.err.println("Error gRPC convertirACualitativa: " + e.getMessage());
            return "—";
        }
    }
}
