package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.pdf.PdfTheme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: ReportesService (Microservicio Secretaría)")
class ReportesServiceTest {

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Mock
    private EstudianteService estudianteService;

    @Mock
    private MatriculaService matriculaService;

    @Mock
    private RepresentanteService representanteService;

    @Mock
    private CatalogoService catalogo;

    private PdfTheme theme;
    private ReportesService reportesService;

    @BeforeEach
    void setUp() {
        theme = new PdfTheme("Escuela Provincias Unidas", "Quevedo, Ecuador");
        reportesService = new ReportesService(jdbc, estudianteService, matriculaService,
                representanteService, catalogo, theme);
    }

    @Test
    @DisplayName("1. Generar Certificado de Matrícula en formato PDF")
    void test_certificadoMatricula_success() throws IOException {
        Map<String, Object> matricula = Map.of(
                "id_matricula", 100L,
                "estudiante", "Pedro Castro",
                "cedula", "1205316456",
                "codigo_estudiante", "EST-2026-001",
                "ano_lectivo", "2026-2027",
                "grado", "Segundo EGB",
                "paralelo", "A",
                "numero_orden", 1,
                "fecha_registro", LocalDate.of(2026, 5, 2),
                "estado", "ACTIVA"
        );
        given(matriculaService.obtenerPorId(100L)).willReturn(matricula);

        byte[] pdf = reportesService.certificadoMatricula(100L);

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("2. Generar Ficha del Estudiante en formato PDF")
    void test_fichaEstudiante_success() throws IOException {
        Map<String, Object> estudiante = Map.of(
                "nombres", "Carlos",
                "apellidos", "Luna",
                "cedula", "1205316456",
                "codigo_estudiante", "EST-002",
                "fecha_nacimiento", LocalDate.of(2015, 3, 10),
                "genero", "MASCULINO",
                "telefono", "0991234567",
                "correo", "cluna@uteq.edu.ec",
                "direccion", "Quevedo Centro"
        );
        given(estudianteService.obtenerPorId(50L)).willReturn(estudiante);

        byte[] pdf = reportesService.fichaEstudiante(50L);

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("3. Generar Nómina de Matrículas en formato PDF")
    @SuppressWarnings("unchecked")
    void test_nominaMatriculas_success() throws IOException {
        Map<String, Object> row = Map.of(
                "numero_orden", 1,
                "fecha_registro", LocalDate.of(2026, 5, 2),
                "estado", "ACTIVA",
                "cedula", "1205316456",
                "estudiante", "Pedro Castro"
        );
        given(jdbc.query(contains("sga_secretaria.matriculas"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(row));
        given(catalogo.anosLectivos()).willReturn(List.of(
                new CatalogoService.AnoLectivo(2026L, "2026-2027", LocalDate.of(2026, 5, 2), LocalDate.of(2027, 2, 28), true)));
        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));
        given(catalogo.paralelos(1L)).willReturn(List.of(new CatalogoService.Paralelo(101L, 1L, "A", true)));

        byte[] pdf = reportesService.nominaMatriculas(2026L, 1L, 101L);

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("4. Generar Libreta de Calificaciones en formato PDF")
    @SuppressWarnings("unchecked")
    void test_libreta_success() throws IOException {
        Map<String, Object> matricula = Map.of(
                "id_matricula", 100L,
                "id_ano_lectivo", 2026L,
                "estudiante", "Pedro Castro",
                "cedula", "1205316456",
                "codigo_estudiante", "EST-001",
                "ano_lectivo", "2026-2027",
                "grado", "Primero EGB",
                "paralelo", "A"
        );
        given(matriculaService.obtenerPorId(100L)).willReturn(matricula);

        Map<String, Object> periodo = Map.of(
                "id_periodo", 1L,
                "nombre", "1er Trimestre",
                "tipo", "TRIMESTRE",
                "fecha_inicio", LocalDate.of(2026, 5, 2),
                "fecha_fin", LocalDate.of(2026, 8, 15)
        );
        given(jdbc.query(contains("sga_docente.periodos_evaluacion"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(periodo));

        Map<String, Object> materia = Map.of(
                "asignatura", "Matemáticas",
                "promedio_formativo", 9.0,
                "nota_sumativa", 8.5,
                "promedio_trimestral", 8.85,
                "nota_cualitativa", "DAR"
        );
        given(jdbc.query(contains("sga_docente.promedios_trimestrales"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(materia));

        byte[] pdf = reportesService.libreta(100L, 1L);

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("5. Generar Reporte de Asistencia Mensual en PDF")
    @SuppressWarnings("unchecked")
    void test_asistenciaMensual_success() throws IOException {
        Map<String, Object> matricula = Map.of(
                "id_matricula", 100L,
                "estudiante", "Pedro Castro",
                "cedula", "1205316456",
                "codigo_estudiante", "EST-001",
                "ano_lectivo", "2026-2027",
                "grado", "Primero EGB",
                "paralelo", "A"
        );
        given(matriculaService.obtenerPorId(100L)).willReturn(matricula);

        Map<String, Object> asistencia = Map.of(
                "fecha", LocalDate.of(2026, 5, 10),
                "asignatura", "Lengua y Literatura",
                "estado", "PRESENTE",
                "justificacion", ""
        );
        given(jdbc.query(contains("sga_docente.asistencias"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(asistencia));

        byte[] pdf = reportesService.asistenciaMensual(100L, "2026-05");

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("6. Lanzar ApiException si el formato de mes de asistencia es inválido")
    void test_asistenciaMensual_invalidMonth_throwsApiException() {
        Map<String, Object> matricula = Map.of("id_matricula", 100L);
        given(matriculaService.obtenerPorId(100L)).willReturn(matricula);

        assertThatThrownBy(() -> reportesService.asistenciaMensual(100L, "mes-invalido"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("El parámetro 'mes' debe tener formato YYYY-MM");
    }

    @Test
    @DisplayName("7. Generar Ficha del Representante en formato PDF")
    void test_fichaRepresentante_success() throws IOException {
        Map<String, Object> rep = Map.of(
                "nombres", "Maria",
                "apellidos", "Lopez",
                "cedula", "1205316456",
                "parentesco", "MADRE",
                "telefono_principal", "0991234567",
                "telefono_alt", "0987654321",
                "correo", "mlopez@gmail.com",
                "direccion", "Quevedo"
        );
        given(representanteService.obtenerPorId(5L)).willReturn(rep);

        byte[] pdf = reportesService.fichaRepresentante(5L);

        assertThat(pdf).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("8. Obtener estadísticas institucionales consolidadas")
    @SuppressWarnings("unchecked")
    void test_estadisticas_success() {
        Map<String, Object> totales = Map.of(
                "total", 100L,
                "activas", 95L,
                "retiradas", 5L,
                "con_discapacidad", 2L,
                "masculino", 50L,
                "femenino", 50L
        );
        given(jdbc.query(contains("COUNT(*) AS total"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(totales));

        Map<String, Object> conGrado = Map.of("id_grado", 1L, "total", 40L);
        given(jdbc.query(contains("GROUP BY id_grado"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(conGrado));

        given(catalogo.grados()).willReturn(List.of(new CatalogoService.Grado(1L, "Primero EGB", 1, true)));

        Map<String, Object> conEstado = Map.of("estado", "ACTIVA", "cantidad", 95L);
        given(jdbc.query(contains("GROUP BY estado"), any(SqlParameterSource.class), any(RowMapper.class)))
                .willReturn(List.of(conEstado));

        Map<String, Object> stats = reportesService.estadisticas(2026L);

        assertThat(stats).isNotNull();
        assertThat(stats.get("totales")).isEqualTo(totales);
        assertThat(stats.get("por_grado")).asList().isNotEmpty();
        assertThat(stats.get("por_estado")).asList().isNotEmpty();
    }
}
