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
import ec.edu.uteq.sga.grpc.principal.ListarRepresentantesPorEstudiantesRequest;
import ec.edu.uteq.sga.grpc.principal.MatriculaProto;
import ec.edu.uteq.sga.grpc.principal.ObtenerEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.ObtenerFichaRequest;
import ec.edu.uteq.sga.grpc.principal.ObtenerMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.ObtenerRepresentanteRequest;
import ec.edu.uteq.sga.grpc.principal.ParaleloProto;
import ec.edu.uteq.sga.grpc.principal.PrincipalServiceGrpc;
import ec.edu.uteq.sga.grpc.principal.RepresentanteProto;
import ec.edu.uteq.sga.grpc.principal.RepresentantesResponse;
import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.TraceContext;
import ec.uteq.sga.secretaria.security.AuthenticatedUser;
import io.grpc.Metadata;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.MetadataUtils;
import jakarta.servlet.http.HttpServletRequest;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Cliente gRPC hacia sga-principal: reemplaza el SQL directo a
 * sga_principal.anos_lectivos/grados/paralelos/asignaturas (catalogo
 * institucional). Mismo patron que AsistenciaGrpcClient/ActividadGrpcClient
 * ya usan en sga-principal para hablar con MICRO-DOCENTE (stub bloqueante +
 * un header de metadata adjuntado antes de cada llamada).
 *
 * Resiliencia: cada llamada tiene deadline de {@link #DEADLINE_SEGUNDOS}s y,
 * si sga-principal esta caido o no responde (UNAVAILABLE/DEADLINE_EXCEEDED),
 * se reintenta hasta {@link #MAX_REINTENTOS} veces con backoff exponencial.
 * Los errores de negocio (NOT_FOUND, ALREADY_EXISTS, INVALID_ARGUMENT...) NO
 * se reintentan, porque repetir la misma llamada no cambia el resultado.
 * Agotados los reintentos: los listados (listar*) se degradan a un resultado
 * vacio para que la pantalla no se caiga; las escrituras (crear/actualizar/
 * cambiarEstado) y los "obtener por id" siguen lanzando error, porque fingir
 * un exito o un registro que no existe seria peor que mostrar el fallo.
 */
@Component
public class PrincipalGrpcClient {

    private static final Logger log = LoggerFactory.getLogger(PrincipalGrpcClient.class);

    private static final Metadata.Key<String> INTERNAL_TOKEN_KEY =
            Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("trace_id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> ACTOR_KEY =
            Metadata.Key.of("actor_username", Metadata.ASCII_STRING_MARSHALLER);

    private static final long DEADLINE_SEGUNDOS = 3;
    private static final int MAX_REINTENTOS = 2;
    private static final long[] BACKOFF_MS = {500, 1000};

    @GrpcClient("principal-service")
    private PrincipalServiceGrpc.PrincipalServiceBlockingStub stub;

    @Value("${app.grpc.internal-token}")
    private String internalToken;

    /**
     * Reenvia el trace_id del request HTTP actual (TraceContext, poblado por
     * TraceIdFilter) y el username autenticado como metadata gRPC, para que
     * sga-principal pueda auditar del otro lado correlacionado con el mismo
     * trace_id y sabiendo quien origino la accion (ver InternalAuthInterceptor
     * en sga-principal).
     */
    private PrincipalServiceGrpc.PrincipalServiceBlockingStub autenticado() {
        Metadata metadata = new Metadata();
        metadata.put(INTERNAL_TOKEN_KEY, internalToken);
        String traceId = TraceContext.current();
        if (traceId != null) metadata.put(TRACE_ID_KEY, traceId);
        String actor = usernameActual();
        if (actor != null) metadata.put(ACTOR_KEY, actor);
        return stub.withInterceptors(MetadataUtils.newAttachHeadersInterceptor(metadata))
                .withDeadlineAfter(DEADLINE_SEGUNDOS, TimeUnit.SECONDS);
    }

    private static String usernameActual() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            AuthenticatedUser user = (AuthenticatedUser) request.getAttribute(AuthenticatedUser.REQUEST_ATTRIBUTE);
            return user != null ? user.username() : null;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /** Ejecuta una llamada gRPC reintentando ante caidas transitorias de sga-principal. */
    private <T> T conReintentos(Supplier<T> llamada) {
        for (int intento = 0; ; intento++) {
            try {
                return llamada.get();
            } catch (StatusRuntimeException e) {
                if (!esTransitorio(e) || intento == MAX_REINTENTOS) throw e;
                log.warn("sga-principal no respondio (intento {}/{}, {}); reintentando en {} ms",
                        intento + 1, MAX_REINTENTOS + 1, e.getStatus().getCode(), BACKOFF_MS[intento]);
                dormir(BACKOFF_MS[intento]);
            }
        }
    }

    /** Como {@link #conReintentos}, pero si agota los reintentos por caida transitoria devuelve {@code valorDegradado} en vez de lanzar. */
    private <T> T conDegradacion(Supplier<T> llamada, T valorDegradado, String descripcion) {
        try {
            return conReintentos(llamada);
        } catch (StatusRuntimeException e) {
            if (!esTransitorio(e)) throw e;
            log.error("sga-principal no respondio tras {} intentos al {}; se degrada a resultado vacio",
                    MAX_REINTENTOS + 1, descripcion, e);
            return valorDegradado;
        }
    }

    private static boolean esTransitorio(StatusRuntimeException e) {
        Status.Code code = e.getStatus().getCode();
        return code == Status.Code.UNAVAILABLE || code == Status.Code.DEADLINE_EXCEEDED;
    }

    private static void dormir(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    public List<AnoLectivoProto> listarAnosLectivos() {
        try {
            return conDegradacion(
                    () -> autenticado().listarAnosLectivos(Empty.newBuilder().build()).getAnosLectivosList(),
                    List.of(), "listar años lectivos");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar años lectivos en sga-principal: " + e.getStatus());
        }
    }

    public List<GradoProto> listarGrados() {
        try {
            return conDegradacion(
                    () -> autenticado().listarGrados(Empty.newBuilder().build()).getGradosList(),
                    List.of(), "listar grados");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar grados en sga-principal: " + e.getStatus());
        }
    }

    public List<ParaleloProto> listarParalelos(Long idGrado) {
        try {
            ListarParalelosRequest request = ListarParalelosRequest.newBuilder()
                    .setIdGrado(idGrado != null ? idGrado : 0)
                    .build();
            return conDegradacion(
                    () -> autenticado().listarParalelos(request).getParalelosList(),
                    List.of(), "listar paralelos");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar paralelos en sga-principal: " + e.getStatus());
        }
    }

    public List<AsignaturaProto> listarAsignaturas() {
        try {
            return conDegradacion(
                    () -> autenticado().listarAsignaturas(Empty.newBuilder().build()).getAsignaturasList(),
                    List.of(), "listar asignaturas");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo consultar asignaturas en sga-principal: " + e.getStatus());
        }
    }

    public EstudianteProto crearEstudiante(GuardarEstudianteRequest request) {
        try {
            return conReintentos(() -> autenticado().crearEstudiante(request));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el estudiante");
        }
    }

    public EstudianteProto actualizarEstudiante(GuardarEstudianteRequest request) {
        try {
            return conReintentos(() -> autenticado().actualizarEstudiante(request));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "actualizar", "el estudiante");
        }
    }

    public ListarEstudiantesResponse listarEstudiantes(ListarEstudiantesRequest request) {
        try {
            return conDegradacion(
                    () -> autenticado().listarEstudiantes(request),
                    ListarEstudiantesResponse.getDefaultInstance(), "listar estudiantes");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo listar estudiantes en sga-principal: " + e.getStatus());
        }
    }

    public EstudianteProto obtenerEstudiante(long id) {
        try {
            return conReintentos(() ->
                    autenticado().obtenerEstudiante(ObtenerEstudianteRequest.newBuilder().setIdEstudiante(id).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "el estudiante");
        }
    }

    public RepresentanteProto obtenerRepresentante(long idRepresentante) {
        try {
            return conReintentos(() -> autenticado().obtenerRepresentante(ObtenerRepresentanteRequest.newBuilder()
                    .setIdRepresentante(idRepresentante).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "el representante");
        }
    }

    public RepresentantesResponse listarRepresentantesPorEstudiantes(List<Long> idsEstudiantes) {
        try {
            return conDegradacion(
                    () -> autenticado().listarRepresentantesPorEstudiantes(
                            ListarRepresentantesPorEstudiantesRequest.newBuilder()
                                    .addAllIdEstudiante(idsEstudiantes).build()),
                    RepresentantesResponse.getDefaultInstance(), "listar representantes por estudiantes");
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "listar", "representantes por estudiantes");
        }
    }

    public void cambiarEstadoEstudiante(long id, boolean activo) {
        try {
            conReintentos(() -> autenticado().cambiarEstadoEstudiante(CambiarEstadoEstudianteRequest.newBuilder()
                    .setIdEstudiante(id).setActivo(activo).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el estudiante");
        }
    }

    public GradoProto crearGrado(GuardarGradoRequest request) {
        try {
            return conReintentos(() -> autenticado().crearGrado(request));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el grado");
        }
    }

    public GradoProto actualizarGrado(GuardarGradoRequest request) {
        try {
            return conReintentos(() -> autenticado().actualizarGrado(request));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "actualizar", "el grado");
        }
    }

    public void cambiarEstadoGrado(long idGrado, boolean activo) {
        try {
            conReintentos(() -> autenticado().cambiarEstadoGrado(CambiarEstadoGradoRequest.newBuilder()
                    .setIdGrado(idGrado).setActivo(activo).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el grado");
        }
    }

    public ParaleloProto crearParalelo(long idGrado, String letra) {
        try {
            return conReintentos(() -> autenticado().crearParalelo(GuardarParaleloRequest.newBuilder()
                    .setIdGrado(idGrado).setLetra(letra).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "el paralelo");
        }
    }

    public void cambiarEstadoParalelo(long idParalelo, boolean activo) {
        try {
            conReintentos(() -> autenticado().cambiarEstadoParalelo(CambiarEstadoParaleloRequest.newBuilder()
                    .setIdParalelo(idParalelo).setActivo(activo).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "el paralelo");
        }
    }

    public MatriculaProto crearMatricula(GuardarMatriculaRequest request) {
        try {
            return conReintentos(() -> autenticado().crearMatricula(request));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "crear", "la matrícula");
        }
    }

    public MatriculaProto obtenerMatricula(long idMatricula) {
        try {
            return conReintentos(() ->
                    autenticado().obtenerMatricula(ObtenerMatriculaRequest.newBuilder().setIdMatricula(idMatricula).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "la matrícula");
        }
    }

    public void cambiarEstadoMatricula(long idMatricula, String estado) {
        try {
            conReintentos(() -> autenticado().cambiarEstadoMatricula(CambiarEstadoMatriculaRequest.newBuilder()
                    .setIdMatricula(idMatricula).setEstado(estado).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "cambiar el estado de", "la matrícula");
        }
    }

    public ListarMatriculasResponse listarMatriculas(ListarMatriculasRequest request) {
        try {
            return conDegradacion(
                    () -> autenticado().listarMatriculas(request),
                    ListarMatriculasResponse.getDefaultInstance(), "listar matrículas");
        } catch (StatusRuntimeException e) {
            throw ApiException.badGateway("No se pudo listar matrículas en sga-principal: " + e.getStatus());
        }
    }

    public FichaProto obtenerFicha(long idEstudiante) {
        try {
            return conReintentos(() ->
                    autenticado().obtenerFicha(ObtenerFichaRequest.newBuilder().setIdEstudiante(idEstudiante).build()));
        } catch (StatusRuntimeException e) {
            throw mapearError(e, "obtener", "la ficha");
        }
    }

    public FichaProto guardarFicha(GuardarFichaRequest request) {
        try {
            return conReintentos(() -> autenticado().guardarFicha(request));
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
