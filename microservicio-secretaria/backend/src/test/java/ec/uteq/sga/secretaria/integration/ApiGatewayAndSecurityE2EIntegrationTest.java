package ec.uteq.sga.secretaria.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import ec.uteq.sga.secretaria.application.service.EstudianteService;
import ec.uteq.sga.secretaria.application.service.MatriculaService;
import ec.uteq.sga.secretaria.application.service.UsuarioService;
import ec.uteq.sga.secretaria.infrastructure.common.GlobalExceptionHandler;
import ec.uteq.sga.secretaria.infrastructure.common.PageResult;
import ec.uteq.sga.secretaria.infrastructure.common.TraceIdFilter;
import ec.uteq.sga.secretaria.infrastructure.security.CurrentUserArgumentResolver;
import ec.uteq.sga.secretaria.infrastructure.security.JwtAuthFilter;
import ec.uteq.sga.secretaria.infrastructure.security.JwtService;
import ec.uteq.sga.secretaria.presentation.controller.EstudianteController;
import ec.uteq.sga.secretaria.presentation.controller.MatriculaController;
import ec.uteq.sga.secretaria.presentation.controller.UsuarioController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Suite de Pruebas de Integración End-to-End (E2E) para el API Gateway (HAProxy)
 * y el Microservicio de Secretaría (Matrículas y Gateway).
 *
 * Valida:
 * 1. Enrutamiento y propagación de cabeceras (X-Gateway, X-Trace-Id) a través del Gateway.
 * 2. Validación y documentación rigurosa del atributo de seguridad: Todo endpoint privado
 *    consultado sin token JWT responde 401 Unauthorized.
 * 3. Control de acceso granular basado en roles (RBAC: SECRETARIA / DIRECTOR vs roles ajenos).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas de Integración E2E: API Gateway (HAProxy) & Seguridad Secretaría")
class ApiGatewayAndSecurityE2EIntegrationTest {

    private MockMvc mockMvc;
    private JwtService jwtService;
    private ObjectMapper objectMapper;

    @Mock
    private EstudianteService estudianteService;

    @Mock
    private MatriculaService matriculaService;

    @Mock
    private UsuarioService usuarioService;

    private String validSecretariaToken;
    private String validDirectorToken;
    private String invalidRoleToken;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        jwtService = new JwtService("test-only-jwt-secret");

        validSecretariaToken = jwtService.generateToken("secretaria.user", List.of("SECRETARIA"));
        validDirectorToken = jwtService.generateToken("director.user", List.of("DIRECTOR"));
        invalidRoleToken = jwtService.generateToken("docente.user", List.of("DOCENTE"));

        EstudianteController estudianteController = new EstudianteController(estudianteService);
        MatriculaController matriculaController = new MatriculaController(matriculaService);
        UsuarioController usuarioController = new UsuarioController(usuarioService);

        // Configuración de la cadena de filtros emulando el Gateway HAProxy y el microservicio
        mockMvc = MockMvcBuilders.standaloneSetup(estudianteController, matriculaController, usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver())
                .addFilters(new TraceIdFilter(), new JwtAuthFilter(jwtService, objectMapper))
                .build();
    }

    // ─── PRUEBA E2E 1: Enrutamiento exitoso vía API Gateway con JWT Válido ───────
    @Test
    @DisplayName("E2E-01: Petición a través del Gateway con token válido es enrutada y responde 200 OK con X-Trace-Id")
    void test_E2E_01_GatewayRouting_SuccessWithValidToken() throws Exception {
        // GIVEN: El servicio retorna una lista paginada de estudiantes
        Map<String, Object> estudiante = Map.of(
                "id_estudiante", 1L,
                "cedula", "1205316456",
                "nombres", "Carlos Leonardo",
                "apellidos", "Castro Mora"
        );
        PageResult<Map<String, Object>> pageResult = PageResult.of(List.of(estudiante), 1L, 1, 15);
        given(estudianteService.listarTodos(any(), eq(1), eq(15))).willReturn(pageResult);

        // WHEN: Se envía la petición con cabeceras simuladas del API Gateway (HAProxy)
        mockMvc.perform(get("/api/secretario/estudiantes")
                        .header("X-Gateway", "HAProxy-SGA")
                        .header("X-Forwarded-For", "192.168.1.100")
                        .header("X-Trace-Id", "gateway-trace-uuid-12345")
                        .header("Authorization", "Bearer " + validSecretariaToken)
                        .accept(MediaType.APPLICATION_JSON))
                // THEN: Responde 200 OK, propaga la cabecera X-Trace-Id y retorna los datos
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "gateway-trace-uuid-12345"))
                .andExpect(jsonPath("$.data[0].cedula").value("1205316456"))
                .andExpect(jsonPath("$.meta.total").value(1));
    }

    // ─── PRUEBA E2E 2: Trazabilidad y Generación de Trace-Id en el Gateway ────────
    @Test
    @DisplayName("E2E-02: Petición sin Trace-Id previo genera automáticamente un X-Trace-Id en la respuesta")
    void test_E2E_02_GatewayTraceability_GeneratesTraceId() throws Exception {
        given(estudianteService.listarTodos(any(), eq(1), eq(15)))
                .willReturn(PageResult.of(List.of(), 0L, 1, 15));

        mockMvc.perform(get("/api/secretario/estudiantes")
                        .header("Authorization", "Bearer " + validSecretariaToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Trace-Id"))
                .andExpect(header().string("X-Trace-Id", notNullValue()));
    }

    // ─── PRUEBA E2E 3: Atributo de Seguridad: 401 Unauthorized sin Token ─────────
    @ParameterizedTest(name = "Endpoint protegido sin token: {0} -> 401 Unauthorized")
    @ValueSource(strings = {
            "/api/secretario/estudiantes",
            "/api/secretario/matriculas",
            "/api/secretario/usuarios"
    })
    @DisplayName("E2E-03a: Atributo de Seguridad -- Petición sin token JWT debe responder 401 Unauthorized")
    void test_E2E_03a_SecurityAttribute_UnauthorizedWithoutToken(String endpoint) throws Exception {
        // WHEN / THEN: Petición sin header Authorization es rechazada con 401 Unauthorized
        mockMvc.perform(get(endpoint)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token no proporcionado"));
    }

    @Test
    @DisplayName("E2E-03b: Atributo de Seguridad -- Petición con token inválido/manipulado responde 401 Unauthorized")
    void test_E2E_03b_SecurityAttribute_UnauthorizedWithInvalidToken() throws Exception {
        String tokenInvalido = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.token_falso_manipulado.firma_invalida";

        mockMvc.perform(get("/api/secretario/estudiantes")
                        .header("Authorization", tokenInvalido)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Token invalido o expirado"));
    }

    // ─── PRUEBA E2E 4: Control de Acceso por Roles (RBAC: 403 Forbidden) ──────────
    @Test
    @DisplayName("E2E-04: Atributo de Seguridad -- Usuario con rol no autorizado (DOCENTE) responde 403 Forbidden")
    void test_E2E_04_SecurityAttribute_ForbiddenWithUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/secretario/estudiantes")
                        .header("Authorization", "Bearer " + invalidRoleToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Acceso denegado"));
    }
}
