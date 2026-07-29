package ec.uteq.sga.secretaria.grpc;

import ec.edu.uteq.sga.grpc.principal.AnoLectivoProto;
import ec.edu.uteq.sga.grpc.principal.AsignaturaProto;
import ec.edu.uteq.sga.grpc.principal.CambiarEstadoEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.CambiarEstadoGradoRequest;
import ec.edu.uteq.sga.grpc.principal.CambiarEstadoMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.CambiarEstadoParaleloRequest;
import ec.edu.uteq.sga.grpc.principal.Empty;
import ec.edu.uteq.sga.grpc.principal.EstudianteProto;
import ec.edu.uteq.sga.grpc.principal.FichaProto;
import ec.edu.uteq.sga.grpc.principal.GradoProto;
import ec.edu.uteq.sga.grpc.principal.GuardarEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.GuardarFichaRequest;
import ec.edu.uteq.sga.grpc.principal.GuardarGradoRequest;
import ec.edu.uteq.sga.grpc.principal.GuardarMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.GuardarParaleloRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesResponse;
import ec.edu.uteq.sga.grpc.principal.ListarMatriculasRequest;
import ec.edu.uteq.sga.grpc.principal.ListarMatriculasResponse;
import ec.edu.uteq.sga.grpc.principal.ListarParalelosRequest;
import ec.edu.uteq.sga.grpc.principal.MatriculaProto;
import ec.edu.uteq.sga.grpc.principal.ObtenerEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.ObtenerFichaRequest;
import ec.edu.uteq.sga.grpc.principal.ObtenerMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.ParaleloProto;
import ec.edu.uteq.sga.grpc.principal.PrincipalServiceGrpc;
import ec.uteq.sga.secretaria.common.ApiException;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente gRPC hacia sga-principal: reemplaza el SQL directo a
 * sga_principal.anos_lectivos/grados/paralelos/asignaturas (catalogo
 * institucional). Mismo patron que AsistenciaGrpcClient/ActividadGrpcClient
 * ya usan en sga-principal para hablar con MICRO-DOCENTE (stub bloqueante +
 * un header de metadata adjuntado antes de cada llamada).
 */
@Component
public class PrincipalGrpcClient {

    private static final Metadata.Key<String> INTERNAL_TOKEN_KEY =
            Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER);

    @GrpcClient("principal-service")
    private PrincipalServiceGrpc.PrincipalServiceBlockingStub stub;

    @Value("${app.grpc.internal-token}")
    private String internalToken;

    private PrincipalServiceGrpc.PrincipalServiceBlockingStub autenticado() {
        Metadata metadata = new Metadata();
        metadata.put(INTERNAL_TOKEN_KEY, internalToken);
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata));
    }

    public List<AnoLectivoProto> listarAnosLectivos() {
        try {
            return autenticado().listarAnosLectivos(Empty.newBuilder().build()).getAnosLectivosList();
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar años lectivos en sga-principal: " + e.getStatus());
        }
    }

    public List<GradoProto> listarGrados() {
        try {
            return autenticado().listarGrados(Empty.newBuilder().build()).getGradosList();
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar grados en sga-principal: " + e.getStatus());
        }
    }

    public List<ParaleloProto> listarParalelos(Long idGrado) {
        try {
            ListarParalelosRequest request = ListarParalelosRequest.newBuilder()
                    .setIdGrado(idGrado != null ? idGrado : 0)
                    .build();
            return autenticado().listarParalelos(request).getParalelosList();
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar paralelos en sga-principal: " + e.getStatus());
        }
    }

    public List<AsignaturaProto> listarAsignaturas() {
        try {
            return autenticado().listarAsignaturas(Empty.newBuilder().build()).getAsignaturasList();
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar asignaturas en sga-principal: " + e.getStatus());
        }
    }

    public EstudianteProto crearEstudiante(GuardarEstudianteRequest request) {
        try {
            return autenticado().crearEstudiante(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el estudiante");
        }
    }

    public EstudianteProto actualizarEstudiante(GuardarEstudianteRequest request) {
        try {
            return autenticado().actualizarEstudiante(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "actualizar", "el estudiante");
        }
    }

    public ListarEstudiantesResponse listarEstudiantes(ListarEstudiantesRequest request) {
        try {
            return autenticado().listarEstudiantes(request);
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo listar estudiantes en sga-principal: " + e.getStatus());
        }
    }

    public EstudianteProto obtenerEstudiante(long id) {
        try {
            return autenticado().obtenerEstudiante(ObtenerEstudianteRequest.newBuilder().setIdEstudiante(id).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "el estudiante");
        }
    }

    public void cambiarEstadoEstudiante(long id, boolean activo) {
        try {
            autenticado().cambiarEstadoEstudiante(CambiarEstadoEstudianteRequest.newBuilder()
                    .setIdEstudiante(id).setActivo(activo).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el estudiante");
        }
    }

    public GradoProto crearGrado(GuardarGradoRequest request) {
        try {
            return autenticado().crearGrado(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el grado");
        }
    }

    public GradoProto actualizarGrado(GuardarGradoRequest request) {
        try {
            return autenticado().actualizarGrado(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "actualizar", "el grado");
        }
    }

    public void cambiarEstadoGrado(long idGrado, boolean activo) {
        try {
            autenticado().cambiarEstadoGrado(CambiarEstadoGradoRequest.newBuilder()
                    .setIdGrado(idGrado).setActivo(activo).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el grado");
        }
    }

    public ParaleloProto crearParalelo(long idGrado, String letra) {
        try {
            return autenticado().crearParalelo(GuardarParaleloRequest.newBuilder()
                    .setIdGrado(idGrado).setLetra(letra).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el paralelo");
        }
    }

    public void cambiarEstadoParalelo(long idParalelo, boolean activo) {
        try {
            autenticado().cambiarEstadoParalelo(CambiarEstadoParaleloRequest.newBuilder()
                    .setIdParalelo(idParalelo).setActivo(activo).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el paralelo");
        }
    }

    public MatriculaProto crearMatricula(GuardarMatriculaRequest request) {
        try {
            return autenticado().crearMatricula(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "la matrícula");
        }
    }

    public MatriculaProto obtenerMatricula(long idMatricula) {
        try {
            return autenticado().obtenerMatricula(ObtenerMatriculaRequest.newBuilder().setIdMatricula(idMatricula).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "la matrícula");
        }
    }

    public void cambiarEstadoMatricula(long idMatricula, String estado) {
        try {
            autenticado().cambiarEstadoMatricula(CambiarEstadoMatriculaRequest.newBuilder()
                    .setIdMatricula(idMatricula).setEstado(estado).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "la matrícula");
        }
    }

    public ListarMatriculasResponse listarMatriculas(ListarMatriculasRequest request) {
        try {
            return autenticado().listarMatriculas(request);
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo listar matrículas en sga-principal: " + e.getStatus());
        }
    }

    public FichaProto obtenerFicha(long idEstudiante) {
        try {
            return autenticado().obtenerFicha(ObtenerFichaRequest.newBuilder().setIdEstudiante(idEstudiante).build());
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "la ficha");
        }
    }

    public FichaProto guardarFicha(GuardarFichaRequest request) {
        try {
            return autenticado().guardarFicha(request);
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "guardar", "la ficha");
        }
    }

    /** Traduce el Status gRPC que devuelve sga-principal (ver PrincipalGrpcService.toGrpcStatus) al ApiException equivalente. */
    private ApiException mapearError(StatusRuntimeException e, String accion, String entidad) {
        String detalle = e.getStatus().getDescription() != null ? e.getStatus().getDescription() : e.getStatus().toString();
        Status.Code code = e.getStatus().getCode();
        if (code == Status.Code.ALREADY_EXISTS) return ApiException.conflict(detalle);
        if (code == Status.Code.NOT_FOUND) return ApiException.notFound(detalle);
        if (code == Status.Code.INVALID_ARGUMENT) return ApiException.badRequest(detalle);
        return ApiException.badGateway("No se pudo " + accion + " " + entidad + " en sga-principal: " + detalle);
    }
}
