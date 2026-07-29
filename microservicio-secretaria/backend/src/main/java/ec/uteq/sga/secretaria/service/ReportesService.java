package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.pdf.CertificadoMatriculaPdfBuilder;
import ec.uteq.sga.secretaria.pdf.FichaEstudiantePdfBuilder;
import ec.uteq.sga.secretaria.pdf.NominaMatriculasPdfBuilder;
import ec.uteq.sga.secretaria.pdf.PdfTheme;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportesService {

    private final NamedParameterJdbcTemplate jdbc;
    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final CatalogoService catalogo;
    private final PdfTheme theme;

    public ReportesService(NamedParameterJdbcTemplate jdbc, EstudianteService estudianteService,
                            MatriculaService matriculaService, CatalogoService catalogo, PdfTheme theme) {
        this.jdbc = jdbc;
        this.estudianteService = estudianteService;
        this.matriculaService = matriculaService;
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
