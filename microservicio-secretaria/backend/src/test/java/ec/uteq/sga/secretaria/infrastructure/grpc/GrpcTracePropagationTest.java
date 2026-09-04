package ec.uteq.sga.secretaria.infrastructure.grpc;

import ec.edu.uteq.sga.grpc.principal.EstudianteProto;
import ec.edu.uteq.sga.grpc.principal.ObtenerEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.PrincipalServiceGrpc;
import ec.uteq.sga.secretaria.infrastructure.common.TraceContext;
import ec.uteq.sga.secretaria.infrastructure.common.TraceIdFilter;
import io.grpc.ManagedChannel;
import io.grpc.Metadata;
import io.grpc.Server;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas de Trazabilidad gRPC: Propagación de TraceId hacia sga-principal")
class GrpcTracePropagationTest {

    private Server server;
    private ManagedChannel channel;
    private PrincipalGrpcClient client;
    private final AtomicReference<Metadata> capturedMetadata = new AtomicReference<>();

    private static final Metadata.Key<String> TRACE_ID_KEY =
            Metadata.Key.of("trace_id", Metadata.ASCII_STRING_MARSHALLER);
    private static final Metadata.Key<String> INTERNAL_TOKEN_KEY =
            Metadata.Key.of("internal_token", Metadata.ASCII_STRING_MARSHALLER);

    @BeforeEach
    void setUp() throws IOException {
        String serverName = InProcessServerBuilder.generateName();

        server = InProcessServerBuilder.forName(serverName)
                .directExecutor()
                .addService(ServerInterceptors.intercept(
                        new PrincipalServiceGrpc.PrincipalServiceImplBase() {
                            @Override
                            public void obtenerEstudiante(ObtenerEstudianteRequest request,
                                                          StreamObserver<EstudianteProto> responseObserver) {
                                responseObserver.onNext(EstudianteProto.newBuilder()
                                        .setIdEstudiante(request.getIdEstudiante())
                                        .setCedula("1205316456")
                                        .setNombres("Carlos")
                                        .setApellidos("Luna")
                                        .build());
                                responseObserver.onCompleted();
                            }
                        },
                        new ServerInterceptor() {
                            @Override
                            public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
                                    ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
                                capturedMetadata.set(headers);
                                return next.startCall(call, headers);
                            }
                        }
                ))
                .build()
                .start();

        channel = InProcessChannelBuilder.forName(serverName).directExecutor().build();
        PrincipalServiceGrpc.PrincipalServiceBlockingStub stub = PrincipalServiceGrpc.newBlockingStub(channel);

        client = new PrincipalGrpcClient();
        ReflectionTestUtils.setField(client, "stub", stub);
        ReflectionTestUtils.setField(client, "internalToken", "secret-internal-token-bcel");
    }

    @AfterEach
    void tearDown() {
        TraceContext.clear();
        if (channel != null && !channel.isShutdown()) {
            channel.shutdownNow();
        }
        if (server != null && !server.isShutdown()) {
            server.shutdownNow();
        }
    }

    @Test
    @DisplayName("1. TraceContext propaga metadata trace_id e internal_token en llamadas gRPC")
    void test_traceContext_propagatesToGrpcMetadata() {
        String testTraceId = "corr-test-trace-uuid-1234";
        TraceContext.set(testTraceId);

        EstudianteProto result = client.obtenerEstudiante(1L);

        assertThat(result).isNotNull();
        assertThat(result.getIdEstudiante()).isEqualTo(1L);

        Metadata headers = capturedMetadata.get();
        assertThat(headers).isNotNull();
        assertThat(headers.get(TRACE_ID_KEY)).isEqualTo(testTraceId);
        assertThat(headers.get(INTERNAL_TOKEN_KEY)).isEqualTo("secret-internal-token-bcel");
    }

    @Test
    @DisplayName("2. TraceIdFilter propaga X-Trace-Id HTTP existente hacia la metadata gRPC saliente")
    void test_traceIdFilter_existingHeader_propagatesToGrpc() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Trace-Id", "gateway-trace-xyz-8888");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            EstudianteProto result = client.obtenerEstudiante(2L);
            assertThat(result).isNotNull();

            Metadata headers = capturedMetadata.get();
            assertThat(headers).isNotNull();
            assertThat(headers.get(TRACE_ID_KEY)).isEqualTo("gateway-trace-xyz-8888");
        });

        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("gateway-trace-xyz-8888");
    }

    @Test
    @DisplayName("3. TraceIdFilter autogenera trace_id si no existe cabecera y lo propaga a gRPC")
    void test_traceIdFilter_generatedHeader_propagatesToGrpc() throws Exception {
        TraceIdFilter filter = new TraceIdFilter();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            EstudianteProto result = client.obtenerEstudiante(3L);
            assertThat(result).isNotNull();

            Metadata headers = capturedMetadata.get();
            assertThat(headers).isNotNull();

            String grpcTraceId = headers.get(TRACE_ID_KEY);
            assertThat(grpcTraceId).isNotBlank();
            assertThat(grpcTraceId).isEqualTo(response.getHeader("X-Trace-Id"));
        });
    }
}
