package ec.uteq.sga.secretaria.application.service;

import ec.edu.uteq.sga.grpc.principal.EstudianteProto;
import ec.edu.uteq.sga.grpc.principal.GuardarEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesResponse;
import ec.edu.uteq.sga.grpc.principal.RepresentanteProto;
import ec.uteq.sga.secretaria.domain.dto.EstudianteRequest;
import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.common.PageResult;
import ec.uteq.sga.secretaria.infrastructure.grpc.PrincipalGrpcClient;
import ec.uteq.sga.secretaria.infrastructure.security.CryptoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: EstudianteService (Microservicio Secretaría)")
class EstudianteServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Mock
    private PrincipalGrpcClient principalGrpcClient;

    @Mock
    private AuditoriaService auditoriaService;

    private CryptoService crypto;
    private EstudianteService estudianteService;

    @BeforeEach
    void setUp() {
        byte[] key32 = new byte[32];
        System.arraycopy("sga-secretaria-test-key-32bytes!".getBytes(), 0, key32, 0, 32);
        crypto = new CryptoService(Base64.getEncoder().encodeToString(key32));

        estudianteService = new EstudianteService(jdbc, crypto, principalGrpcClient, auditoriaService);
    }

    @Test
    @DisplayName("1. Listar estudiantes paginados vía gRPC y descifrado de campos sensibles")
    void test_listarTodos_success() {
        String encryptedDir = crypto.encrypt("Av. Universitaria 100");
        String encryptedTel = crypto.encrypt("0998887766");

        EstudianteProto proto = EstudianteProto.newBuilder()
                .setIdEstudiante(1L)
                .setCedula("1205316456")
                .setNombres("Carlos Leonardo")
                .setApellidos("Castro Lopez")
                .setCorreo("ccastro@uteq.edu.ec")
                .setDireccion(encryptedDir)
                .setTelefono(encryptedTel)
                .setEstado("ACTIVO")
                .build();

        ListarEstudiantesResponse response = ListarEstudiantesResponse.newBuilder()
                .addAllEstudiantes(List.of(proto))
                .setTotal(1)
                .build();

        given(principalGrpcClient.listarEstudiantes(any(ListarEstudiantesRequest.class))).willReturn(response);

        PageResult<Map<String, Object>> result = estudianteService.listarTodos("Carlos", 1, 15);

        assertThat(result).isNotNull();
        assertThat(result.meta().total()).isEqualTo(1);
        assertThat(result.data()).hasSize(1);

        Map<String, Object> item = result.data().get(0);
        assertThat(item.get("cedula")).isEqualTo("1205316456");
        assertThat(item.get("direccion")).isEqualTo("Av. Universitaria 100");
        assertThat(item.get("telefono")).isEqualTo("0998887766");
        assertThat(item.get("estado")).isEqualTo(true);
    }

    @Test
    @DisplayName("2. Obtener estudiante por ID con enriquecimiento de representante y ficha médica")
    @SuppressWarnings("unchecked")
    void test_obtenerPorId_withRepresentanteAndFicha() {
        EstudianteProto proto = EstudianteProto.newBuilder()
                .setIdEstudiante(10L)
                .setCedula("1205316456")
                .setNombres("Carlos")
                .setApellidos("Castro")
                .setIdRepresentante(5L)
                .setEstado("ACTIVA")
                .build();

        RepresentanteProto repProto = RepresentanteProto.newBuilder()
                .setIdRepresentante(5L)
                .setCedula("1201122334")
                .setDireccion("Calle Central 456")
                .setCorreo("padre@correo.com")
                .setIngresoMensual(750.0)
                .build();

        given(principalGrpcClient.obtenerEstudiante(10L)).willReturn(proto);

        Map<String, Object> mutableFicha = new LinkedHashMap<>();
        mutableFicha.put("tipo_sangre", "O+");
        mutableFicha.put("alergias", crypto.encrypt("Penicilina"));
        mutableFicha.put("enfermedad_catastrofica", false);

        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(mutableFicha));
        given(principalGrpcClient.obtenerRepresentante(5L)).willReturn(repProto);

        Map<String, Object> result = estudianteService.obtenerPorId(10L);

        assertThat(result).isNotNull();
        assertThat(result.get("cedula")).isEqualTo("1205316456");
        assertThat(result.get("rep_cedula")).isEqualTo("1201122334");
        assertThat(result.get("rep_correo")).isEqualTo("padre@correo.com");
        assertThat(result.get("tipo_sangre")).isEqualTo("O+");
        assertThat(result.get("alergias")).isEqualTo("Penicilina");
        assertThat(result.get("estado")).isEqualTo(true);
    }

    @Test
    @DisplayName("3. Crear estudiante exitosamente vía gRPC y cálculo de código secuencial")
    @SuppressWarnings("unchecked")
    void test_crear_success() {
        EstudianteRequest req = new EstudianteRequest(
                "1205316456", "Carlos Leonardo", "Castro Lopez", "2005-08-15",
                "MASCULINO", "ccastro@uteq.edu.ec", "Av. Siempre Viva 123", "0991234567",
                true, "AUDITIVA", 25, 5L, "ECUATORIANA", "MESTIZO", "Quevedo",
                "PADRES", 2, true, "CONADIS-1234", "http://foto.jpg"
        );

        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of()) // No duplicados
                .willReturn(List.of(1L)) // Resolver creador por username
                .willReturn(List.of("EST-0010")); // Ultimo código

        EstudianteProto creadoProto = EstudianteProto.newBuilder()
                .setIdEstudiante(100L)
                .setCedula("1205316456")
                .setNombres("Carlos Leonardo")
                .setApellidos("Castro Lopez")
                .setEstado("ACTIVO")
                .setDiscapacidad(true)
                .setPorcentajeDisc(25)
                .build();

        given(principalGrpcClient.crearEstudiante(any(GuardarEstudianteRequest.class))).willReturn(creadoProto);

        Map<String, Object> result = estudianteService.crear(req, "secretaria1");

        assertThat(result).isNotNull();
        assertThat(result.get("id_estudiante")).isEqualTo(100L);
        assertThat(result.get("nombres")).isEqualTo("Carlos Leonardo");
        verify(auditoriaService).registrarCrud(eq("CREAR"), eq("estudiante"), eq(100L), anyString());
    }

    @Test
    @DisplayName("4. Crear estudiante con cédula duplicada lanza ApiException CONFLICT")
    @SuppressWarnings("unchecked")
    void test_crear_duplicateCedula_throwsConflict() {
        EstudianteRequest req = new EstudianteRequest(
                "1205316456", "Carlos", "Castro", "2005-08-15",
                "MASCULINO", "ccastro@uteq.edu.ec", null, null,
                false, null, null, null, null, null, null, null, null, null, null, null
        );

        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(Map.of("id_estudiante", 99L)));

        assertThatThrownBy(() -> estudianteService.crear(req, "secretaria1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ya existe un estudiante con esa cédula");
    }

    @Test
    @DisplayName("5. Actualizar estudiante existente vía gRPC")
    @SuppressWarnings("unchecked")
    void test_actualizar_success() {
        EstudianteProto existente = EstudianteProto.newBuilder()
                .setIdEstudiante(20L)
                .setCedula("1205316456")
                .setNombres("Carlos")
                .setApellidos("Castro")
                .setFechaNacimiento("2005-08-15")
                .setGenero("MASCULINO")
                .setCorreo("ccastro@uteq.edu.ec")
                .setDireccion(crypto.encrypt("Calle A"))
                .setTelefono(crypto.encrypt("0990000000"))
                .setEstado("ACTIVO")
                .build();

        given(principalGrpcClient.obtenerEstudiante(20L)).willReturn(existente);
        given(jdbc.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of()); // Sin ficha

        EstudianteRequest updateReq = new EstudianteRequest(
                "1205316456", "Carlos Modificado", "Castro Lopez", "2005-08-15",
                "MASCULINO", "carlos.mod@uteq.edu.ec", "Calle B 789", "0991112233",
                false, null, 0, null, "ECUATORIANA", "MESTIZO", "Quevedo",
                "PADRES", 1, false, null, null
        );

        EstudianteProto actualizadoProto = EstudianteProto.newBuilder()
                .setIdEstudiante(20L)
                .setCedula("1205316456")
                .setNombres("Carlos Modificado")
                .setApellidos("Castro Lopez")
                .setCorreo("carlos.mod@uteq.edu.ec")
                .setEstado("ACTIVO")
                .build();

        given(principalGrpcClient.actualizarEstudiante(any(GuardarEstudianteRequest.class))).willReturn(actualizadoProto);

        Map<String, Object> result = estudianteService.actualizar(20L, updateReq);

        assertThat(result).isNotNull();
        assertThat(result.get("nombres")).isEqualTo("Carlos Modificado");
        verify(auditoriaService).registrarCrud(eq("EDITAR"), eq("estudiante"), eq(20L), anyString());
    }

    @Test
    @DisplayName("6. Cambiar estado de estudiante vía gRPC")
    void test_cambiarEstado_success() {
        estudianteService.cambiarEstado(10L, false);

        verify(principalGrpcClient).cambiarEstadoEstudiante(10L, false);
    }
}
