package ec.uteq.sga.secretaria.application.service;

import ec.edu.uteq.sga.grpc.principal.GuardarMatriculaRequest;
import ec.edu.uteq.sga.grpc.principal.ListarMatriculasRequest;
import ec.edu.uteq.sga.grpc.principal.ListarMatriculasResponse;
import ec.edu.uteq.sga.grpc.principal.MatriculaProto;
import ec.uteq.sga.secretaria.domain.dto.MatriculaRequest;
import ec.uteq.sga.secretaria.infrastructure.common.PageResult;
import ec.uteq.sga.secretaria.infrastructure.grpc.PrincipalGrpcClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: MatriculaService (Microservicio Secretaría)")
class MatriculaServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Mock
    private CatalogoService catalogo;

    @Mock
    private PrincipalGrpcClient client;

    private MatriculaService matriculaService;

    @BeforeEach
    void setUp() {
        matriculaService = new MatriculaService(jdbc, catalogo, client);
    }

    @Test
    @DisplayName("1. Filtrar y ordenar paralelos activos por grado")
    void test_paralelosPorGrado_success() {
        List<CatalogoService.Paralelo> mockParalelos = List.of(
                new CatalogoService.Paralelo(102L, 1L, "B", true),
                new CatalogoService.Paralelo(101L, 1L, "A", true),
                new CatalogoService.Paralelo(103L, 1L, "C", false)
        );
        given(catalogo.paralelos(1L)).willReturn(mockParalelos);

        List<Map<String, Object>> result = matriculaService.paralelosPorGrado(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).get("letra")).isEqualTo("A");
        assertThat(result.get(0).get("id_paralelo")).isEqualTo(101L);
        assertThat(result.get(1).get("letra")).isEqualTo("B");
        assertThat(result.get(1).get("id_paralelo")).isEqualTo(102L);
    }

    @Test
    @DisplayName("2. Listar matrículas por año lectivo con enriquecimiento de catálogo")
    void test_listarPorAnoLectivo_success() {
        MatriculaProto proto = MatriculaProto.newBuilder()
                .setIdMatricula(500L)
                .setIdEstudiante(10L)
                .setEstudianteNombres("Juan")
                .setEstudianteApellidos("Pérez")
                .setEstudianteCedula("1205316456")
                .setIdGrado(1L)
                .setIdParalelo(101L)
                .setIdAnoLectivo(2026L)
                .setNumeroOrden(1)
                .setFechaRegistro("2026-05-02")
                .setEstado("ACTIVA")
                .build();

        ListarMatriculasResponse response = ListarMatriculasResponse.newBuilder()
                .addAllMatriculas(List.of(proto))
                .setTotal(1)
                .build();

        given(client.listarMatriculas(any(ListarMatriculasRequest.class))).willReturn(response);
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(null)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));
        given(catalogo.anosLectivos()).willReturn(List.of(new CatalogoService.AnoLectivo(2026L, "2026-2027",
                LocalDate.of(2026, 5, 2), LocalDate.of(2027, 2, 28), true)));

        PageResult<Map<String, Object>> result = matriculaService.listarPorAnoLectivo(2026L, 1, 10, "Juan");

        assertThat(result).isNotNull();
        assertThat(result.meta().total()).isEqualTo(1);
        assertThat(result.data()).hasSize(1);

        Map<String, Object> fila = result.data().get(0);
        assertThat(fila.get("id_matricula")).isEqualTo(500L);
        assertThat(fila.get("estudiante")).isEqualTo("Juan Pérez");
        assertThat(fila.get("grado")).isEqualTo("Primero EGB");
        assertThat(fila.get("paralelo")).isEqualTo("A");
        assertThat(fila.get("ano_lectivo")).isEqualTo("2026-2027");
    }

    @Test
    @DisplayName("3. Estadísticas de matrículas por grado y paralelo")
    void test_estadisticasPorGrado_success() {
        MatriculaProto m1 = MatriculaProto.newBuilder()
                .setIdMatricula(1L).setIdGrado(1L).setIdParalelo(101L).setEstado("ACTIVA").build();
        MatriculaProto m2 = MatriculaProto.newBuilder()
                .setIdMatricula(2L).setIdGrado(1L).setIdParalelo(101L).setEstado("RETIRADA").build();

        ListarMatriculasResponse response = ListarMatriculasResponse.newBuilder()
                .addAllMatriculas(List.of(m1, m2))
                .setTotal(2)
                .build();

        given(client.listarMatriculas(any(ListarMatriculasRequest.class))).willReturn(response);
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(1L)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));

        List<Map<String, Object>> stats = matriculaService.estadisticasPorGrado(2026L);

        assertThat(stats).hasSize(1);
        Map<String, Object> stat = stats.get(0);
        assertThat(stat.get("grado")).isEqualTo("Primero EGB");
        assertThat(stat.get("paralelo")).isEqualTo("A");
        assertThat(stat.get("total")).isEqualTo(2L);
        assertThat(stat.get("activas")).isEqualTo(1L);
        assertThat(stat.get("retiradas")).isEqualTo(1L);
    }

    @Test
    @DisplayName("4. Listar matrículas por estudiante enriquecido con historial de promoción")
    @SuppressWarnings("unchecked")
    void test_listarPorEstudiante_success() {
        MatriculaProto proto = MatriculaProto.newBuilder()
                .setIdMatricula(500L)
                .setIdEstudiante(10L)
                .setIdGrado(1L)
                .setIdParalelo(101L)
                .setIdAnoLectivo(2026L)
                .setEstado("ACTIVA")
                .build();

        ListarMatriculasResponse response = ListarMatriculasResponse.newBuilder()
                .addAllMatriculas(List.of(proto))
                .setTotal(1)
                .build();

        given(client.listarMatriculas(any(ListarMatriculasRequest.class))).willReturn(response);
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(null)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));
        given(catalogo.anosLectivos()).willReturn(List.of(new CatalogoService.AnoLectivo(2026L, "2026-2027",
                LocalDate.of(2026, 5, 2), LocalDate.of(2027, 2, 28), true)));

        given(jdbc.query(contains("historial_promocion"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(Map.of("id_matricula", 500L, "resultado_promocion", "PROMOVIDO", "promedio_anual", 9.8)));

        List<Map<String, Object>> result = matriculaService.listarPorEstudiante(10L);

        assertThat(result).hasSize(1);
        Map<String, Object> fila = result.get(0);
        assertThat(fila.get("id_matricula")).isEqualTo(500L);
        assertThat(fila.get("resultado_promocion")).isEqualTo("PROMOVIDO");
        assertThat(fila.get("promedio_anual")).isEqualTo(9.8);
    }

    @Test
    @DisplayName("5. Obtener matrícula por ID")
    void test_obtenerPorId_success() {
        MatriculaProto proto = MatriculaProto.newBuilder()
                .setIdMatricula(500L)
                .setIdEstudiante(10L)
                .setIdGrado(1L)
                .setIdParalelo(101L)
                .setIdAnoLectivo(2026L)
                .setEstado("ACTIVA")
                .build();

        given(client.obtenerMatricula(500L)).willReturn(proto);
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(null)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));
        given(catalogo.anosLectivos()).willReturn(List.of(new CatalogoService.AnoLectivo(2026L, "2026-2027",
                LocalDate.of(2026, 5, 2), LocalDate.of(2027, 2, 28), true)));

        Map<String, Object> result = matriculaService.obtenerPorId(500L);

        assertThat(result).isNotNull();
        assertThat(result.get("id_matricula")).isEqualTo(500L);
        assertThat(result.get("grado")).isEqualTo("Primero EGB");
    }

    @Test
    @DisplayName("6. Crear matrícula y emitir notificación al usuario registrador")
    @SuppressWarnings("unchecked")
    void test_crear_success() {
        MatriculaRequest req = new MatriculaRequest(10L, 1L, 101L, 2026L, "ACTIVA", "Matrícula ordinaria");

        MatriculaProto protoCreada = MatriculaProto.newBuilder()
                .setIdMatricula(777L)
                .setIdEstudiante(10L)
                .setEstudianteNombres("Ana")
                .setEstudianteApellidos("Torres")
                .setIdGrado(1L)
                .setIdParalelo(101L)
                .setIdAnoLectivo(2026L)
                .setEstado("ACTIVA")
                .build();

        given(jdbc.query(contains("sga_principal.usuarios"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(42L));
        given(client.crearMatricula(any(GuardarMatriculaRequest.class))).willReturn(protoCreada);
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(null)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));
        given(catalogo.anosLectivos()).willReturn(List.of(new CatalogoService.AnoLectivo(2026L, "2026-2027",
                LocalDate.of(2026, 5, 2), LocalDate.of(2027, 2, 28), true)));

        Map<String, Object> resultado = matriculaService.crear(req, "secretaria1");

        assertThat(resultado).isNotNull();
        assertThat(resultado.get("id_matricula")).isEqualTo(777L);
        verify(jdbc).update(contains("sga_principal.notificaciones"), any(SqlParameterSource.class));
    }

    @Test
    @DisplayName("7. Cambiar estado de matrícula vía gRPC")
    void test_cambiarEstado_success() {
        matriculaService.cambiarEstado(500L, "RETIRADA");
        verify(client).cambiarEstadoMatricula(500L, "RETIRADA");
    }
}
