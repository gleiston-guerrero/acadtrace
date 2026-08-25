package ec.uteq.sga.secretaria.application.service;

import ec.uteq.sga.secretaria.infrastructure.common.ApiException;
import ec.uteq.sga.secretaria.infrastructure.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.infrastructure.pdf.AsistenciaMensualPdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.CertificadoMatriculaPdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.FichaEstudiantePdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.FichaRepresentantePdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.LibretaCalificacionesPdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.NominaMatriculasPdfBuilder;
import ec.uteq.sga.secretaria.infrastructure.pdf.PdfTheme;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ReportesService {

    private final NamedParameterJdbcTemplate jdbc;
    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final RepresentanteService representanteService;
    private final CatalogoService catalogo;
    private final PdfTheme theme;

    public ReportesService(NamedParameterJdbcTemplate jdbc, EstudianteService estudianteService,
                            MatriculaService matriculaService, RepresentanteService representanteService,
                            CatalogoService catalogo, PdfTheme theme) {
        this.jdbc = jdbc;
        this.estudianteService = estudianteService;
        this.matriculaService = matriculaService;
        this.representanteService = representanteService;
        this.catalogo = catalogo;
        this.theme = theme;
    }

    public byte[] certificadoMatricula(long idMatricula) throws IOException {
        Map<String, Object> matricula = matriculaService.obtenerPorId(idMatricula);
        return CertificadoMatriculaPdfBuilder.build(matricula, theme);
    }

    public byte[] fichaEstudiante(long idEstudiante) throws IOException {
        Map<String, Object> estudiante = estudianteService.obtenerPorId(idEstudiante);
        return FichaEstudiantePdfBuilder.build(estudiante, theme);
    }

    public byte[] nominaMatriculas(long idAno, Long idGrado, Long idParalelo) throws IOException {
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("idAno", idAno);
        String where = "WHERE m.id_ano_lectivo = :idAno";
        if (idGrado != null) {
            params.addValue("idGrado", idGrado);
            where += " AND m.id_grado = :idGrado";
        }
        if (idParalelo != null) {
            params.addValue("idParalelo", idParalelo);
            where += " AND m.id_paralelo = :idParalelo";
        }

        String sql = """
                SELECT m.numero_orden, m.fecha_registro, m.estado,
                       e.cedula, e.nombres || ' ' || e.apellidos AS estudiante
                FROM sga_secretaria.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                %s
                ORDER BY e.apellidos
                """.formatted(where);
        List<Map<String, Object>> matriculas = jdbc.query(sql, params, GenericRowMapper.INSTANCE);

        String anoNombre = catalogo.anosLectivos().stream()
                .filter(a -> a.id() == idAno)
                .findFirst().map(CatalogoService.AnoLectivo::nombre).orElse("");

        String gradoNombre = "";
        if (idGrado != null) {
            long idGradoValue = idGrado;
            gradoNombre = catalogo.grados().stream()
                    .filter(g -> g.id() == idGradoValue)
                    .findFirst().map(CatalogoService.Grado::nombre).orElse("");
        }
        String paraleloLetra = "";
        if (idParalelo != null) {
            long idParaleloValue = idParalelo;
            paraleloLetra = catalogo.paralelos(idGrado).stream()
                    .filter(p -> p.id() == idParaleloValue)
                    .findFirst().map(CatalogoService.Paralelo::letra).orElse("");
        }

        return NominaMatriculasPdfBuilder.build(matriculas, anoNombre, gradoNombre, paraleloLetra, theme);
    }

    /**
     * Datos de calificaciones vienen de sga_docente (microservicio-docente): promedios_trimestrales
     * ligados a periodos_evaluacion (no un entero 1/2/3), y a asignaciones/asignaturas de sga_principal
     * para el nombre de la materia. Se lee por SQL directo, igual que historial_promocion.
     */
    public byte[] libreta(long idMatricula, Long idPeriodo) throws IOException {
        Map<String, Object> matricula = matriculaService.obtenerPorId(idMatricula);
        Map<String, Object> periodo = resolverPeriodo(idPeriodo, (Long) matricula.get("id_ano_lectivo"));

        List<Map<String, Object>> materias = jdbc.query("""
                SELECT a.nombre AS asignatura, pt.promedio_formativo, pt.nota_sumativa,
                       pt.promedio_trimestral, pt.nota_cualitativa
                FROM sga_docente.promedios_trimestrales pt
                JOIN sga_principal.asignaciones asg ON asg.id_asignacion = pt.id_asignacion
                JOIN sga_principal.asignaturas a ON a.id_asignatura = asg.id_asignatura
                WHERE pt.id_matricula = :idMatricula AND pt.id_periodo = :idPeriodo
                ORDER BY a.nombre
                """,
                new MapSqlParameterSource()
                        .addValue("idMatricula", idMatricula)
                        .addValue("idPeriodo", periodo.get("id_periodo")),
                GenericRowMapper.INSTANCE);

        return LibretaCalificacionesPdfBuilder.build(matricula, periodo, materias, theme);
    }

    private Map<String, Object> resolverPeriodo(Long idPeriodo, Long idAnoLectivo) {
        if (idPeriodo != null) {
            List<Map<String, Object>> rows = jdbc.query(
                    "SELECT id_periodo, nombre, tipo, fecha_inicio, fecha_fin FROM sga_docente.periodos_evaluacion WHERE id_periodo = :id",
                    new MapSqlParameterSource("id", idPeriodo), GenericRowMapper.INSTANCE);
            if (rows.isEmpty()) throw ApiException.notFound("Período de evaluación no encontrado");
            return rows.get(0);
        }
        List<Map<String, Object>> rows = jdbc.query("""
                SELECT id_periodo, nombre, tipo, fecha_inicio, fecha_fin
                FROM sga_docente.periodos_evaluacion
                WHERE id_ano_lectivo = :idAno AND activo = true
                ORDER BY fecha_inicio DESC LIMIT 1
                """, new MapSqlParameterSource("idAno", idAnoLectivo), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("No hay un período de evaluación activo para este año lectivo");
        return rows.get(0);
    }

    public byte[] asistenciaMensual(long idMatricula, String mes) throws IOException {
        Map<String, Object> matricula = matriculaService.obtenerPorId(idMatricula);

        YearMonth ym;
        try {
            ym = YearMonth.parse(mes);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("El parámetro 'mes' debe tener formato YYYY-MM");
        }
        LocalDate inicio = ym.atDay(1);
        LocalDate fin = ym.atEndOfMonth();

        List<Map<String, Object>> registros = jdbc.query("""
                SELECT a.fecha, asig.nombre AS asignatura, a.estado, a.justificacion
                FROM sga_docente.asistencias a
                JOIN sga_principal.asignaciones asg ON asg.id_asignacion = a.id_asignacion
                JOIN sga_principal.asignaturas asig ON asig.id_asignatura = asg.id_asignatura
                WHERE a.id_matricula = :idMatricula AND a.fecha BETWEEN :inicio AND :fin
                ORDER BY a.fecha, asig.nombre
                """,
                new MapSqlParameterSource()
                        .addValue("idMatricula", idMatricula)
                        .addValue("inicio", inicio)
                        .addValue("fin", fin),
                GenericRowMapper.INSTANCE);

        String mesLabel = ym.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "EC")) + " " + ym.getYear();
        return AsistenciaMensualPdfBuilder.build(matricula, mesLabel, registros, theme);
    }

    public byte[] fichaRepresentante(long idRepresentante) throws IOException {
        Map<String, Object> representante = representanteService.obtenerPorId(idRepresentante);
        return FichaRepresentantePdfBuilder.build(representante, theme);
    }

    public Map<String, Object> estadisticas(long idAno) {
        MapSqlParameterSource params = new MapSqlParameterSource("idAno", idAno);

        Map<String, Object> totales = jdbc.query("""
                SELECT COUNT(*) AS total,
                       COUNT(*) FILTER (WHERE m.estado = 'ACTIVA') AS activas,
                       COUNT(*) FILTER (WHERE m.estado = 'RETIRADA') AS retiradas,
                       COUNT(*) FILTER (WHERE e.discapacidad = true) AS con_discapacidad,
                       COUNT(*) FILTER (WHERE e.genero = 'MASCULINO') AS masculino,
                       COUNT(*) FILTER (WHERE e.genero = 'FEMENINO') AS femenino
                FROM sga_secretaria.matriculas m
                JOIN sga_secretaria.estudiantes e ON e.id_estudiante = m.id_estudiante
                WHERE m.id_ano_lectivo = :idAno
                """, params, GenericRowMapper.INSTANCE).get(0);

        List<Map<String, Object>> conteosPorGrado = jdbc.query("""
                SELECT id_grado, COUNT(*) AS total
                FROM sga_secretaria.matriculas
                WHERE id_ano_lectivo = :idAno
                GROUP BY id_grado
                """, params, GenericRowMapper.INSTANCE);
        Map<Long, Long> totalPorGrado = new LinkedHashMap<>();
        for (Map<String, Object> c : conteosPorGrado) {
            totalPorGrado.put(((Number) c.get("id_grado")).longValue(), ((Number) c.get("total")).longValue());
        }

        List<Map<String, Object>> porGrado = catalogo.grados().stream()
                .filter(CatalogoService.Grado::activo)
                .sorted(java.util.Comparator.comparingInt(CatalogoService.Grado::orden))
                .map(g -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("grado", g.nombre());
                    row.put("orden", g.orden());
                    row.put("total", totalPorGrado.getOrDefault(g.id(), 0L));
                    return row;
                })
                .toList();

        List<Map<String, Object>> porEstado = jdbc.query("""
                SELECT estado, COUNT(*) AS cantidad
                FROM sga_secretaria.matriculas WHERE id_ano_lectivo = :idAno
                GROUP BY estado
                """, params, GenericRowMapper.INSTANCE);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totales", totales);
        result.put("por_grado", porGrado);
        result.put("por_estado", porEstado);
        return result;
    }
}
