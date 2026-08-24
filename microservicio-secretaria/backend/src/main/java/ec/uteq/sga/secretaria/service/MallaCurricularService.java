package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.ActualizarHorasGradoRequest;
import ec.uteq.sga.secretaria.dto.MallaRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class MallaCurricularService {

    private final NamedParameterJdbcTemplate jdbc;

    public MallaCurricularService(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> porGrado(long idGrado, long idAnoLectivo) {
        String sqlMalla = """
                SELECT m.id_malla, m.id_asignatura, a.nombre AS asignatura, a.codigo,
                       COALESCE(m.horas_semana, a.horas_semana, 4) AS horas_semana,
                       m.dias_semana, m.duracion
                FROM sga_principal.malla_curricular m
                JOIN sga_principal.asignaturas a ON a.id_asignatura = m.id_asignatura
                WHERE m.id_grado = :idGrado AND m.id_ano_lectivo = :idAnoLectivo
                ORDER BY a.nombre ASC
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idGrado", idGrado)
                .addValue("idAnoLectivo", idAnoLectivo);

        List<Map<String, Object>> rowsMalla = jdbc.query(sqlMalla, params, GenericRowMapper.INSTANCE);

        String sqlAsignaciones = """
                SELECT asig.id_asignatura, a.nombre AS asignatura, a.codigo,
                       COALESCE(asig.horas_semanales, 4) AS horas_semanales,
                       TRIM(CONCAT(COALESCE(p.nombres, ''), ' ', COALESCE(p.apellidos, ''))) AS docente_nombre
                FROM sga_principal.asignaciones asig
                JOIN sga_principal.asignaturas a ON a.id_asignatura = asig.id_asignatura
                LEFT JOIN sga_principal.usuarios u ON u.id_usuario = asig.id_docente
                LEFT JOIN sga_principal.personas p ON p.id_usuario = u.id_usuario
                WHERE asig.id_grado = :idGrado AND asig.id_ano_lectivo = :idAnoLectivo AND asig.activo = true
                """;

        List<Map<String, Object>> rowsAsig = jdbc.query(sqlAsignaciones, params, GenericRowMapper.INSTANCE);

        Map<Long, Map<String, Object>> mapaMaterias = new LinkedHashMap<>();

        for (Map<String, Object> m : rowsMalla) {
            Long idAsig = ((Number) m.get("id_asignatura")).longValue();
            Map<String, Object> item = new HashMap<>();
            item.put("idMalla", m.get("id_malla"));
            item.put("idAsignatura", idAsig);
            item.put("asignatura", m.get("asignatura"));
            item.put("codigo", m.get("codigo"));
            item.put("horasSemana", m.get("horas_semana") != null ? m.get("horas_semana") : 4);
            item.put("diasSemana", m.get("dias_semana"));
            item.put("duracion", m.get("duracion"));
            item.put("docentes", new ArrayList<String>());
            item.put("origen", "MALLA");
            mapaMaterias.put(idAsig, item);
        }

        for (Map<String, Object> a : rowsAsig) {
            Long idAsig = ((Number) a.get("id_asignatura")).longValue();
            String nombreDoc = (String) a.get("docente_nombre");
            int horas = a.get("horas_semanales") != null ? ((Number) a.get("horas_semanales")).intValue() : 4;

            if (mapaMaterias.containsKey(idAsig)) {
                Map<String, Object> item = mapaMaterias.get(idAsig);
                @SuppressWarnings("unchecked")
                List<String> docs = (List<String>) item.get("docentes");
                if (nombreDoc != null && !nombreDoc.isBlank() && !docs.contains(nombreDoc)) {
                    docs.add(nombreDoc);
                }
                int hActual = Integer.parseInt(item.get("horasSemana").toString());
                if (horas > hActual) item.put("horasSemana", horas);
            } else {
                Map<String, Object> item = new HashMap<>();
                item.put("idMalla", null);
                item.put("idAsignatura", idAsig);
                item.put("asignatura", a.get("asignatura"));
                item.put("codigo", a.get("codigo"));
                item.put("horasSemana", horas);
                List<String> docs = new ArrayList<>();
                if (nombreDoc != null && !nombreDoc.isBlank()) docs.add(nombreDoc);
                item.put("docentes", docs);
                item.put("origen", "ASIGNACION");
                mapaMaterias.put(idAsig, item);
            }
        }

        List<Map<String, Object>> items = new ArrayList<>(mapaMaterias.values());
        int total = items.stream().mapToInt(i -> Integer.parseInt(i.get("horasSemana").toString())).sum();

        return Map.of("totalHoras", total, "materias", items);
    }

    public Map<Long, Map<String, Object>> resumenGrados(long idAnoLectivo) {
        String sqlMallas = """
                SELECT id_grado, id_asignatura, COALESCE(horas_semana, 4) AS horas
                FROM sga_principal.malla_curricular
                WHERE id_ano_lectivo = :idAnoLectivo
                """;
        List<Map<String, Object>> mallas = jdbc.query(sqlMallas,
                new MapSqlParameterSource("idAnoLectivo", idAnoLectivo), GenericRowMapper.INSTANCE);

        String sqlAsigs = """
                SELECT id_grado, id_asignatura, COALESCE(horas_semanales, 4) AS horas
                FROM sga_principal.asignaciones
                WHERE id_ano_lectivo = :idAnoLectivo AND activo = true
                """;
        List<Map<String, Object>> asigs = jdbc.query(sqlAsigs,
                new MapSqlParameterSource("idAnoLectivo", idAnoLectivo), GenericRowMapper.INSTANCE);

        Map<Long, Map<Long, Integer>> mapaGrados = new HashMap<>();

        for (Map<String, Object> m : mallas) {
            Long idG = ((Number) m.get("id_grado")).longValue();
            Long idA = ((Number) m.get("id_asignatura")).longValue();
            int h = ((Number) m.get("horas")).intValue();
            mapaGrados.computeIfAbsent(idG, k -> new HashMap<>()).put(idA, h);
        }

        for (Map<String, Object> a : asigs) {
            Long idG = ((Number) a.get("id_grado")).longValue();
            Long idA = ((Number) a.get("id_asignatura")).longValue();
            int h = ((Number) a.get("horas")).intValue();
            mapaGrados.computeIfAbsent(idG, k -> new HashMap<>());
            Map<Long, Integer> mapG = mapaGrados.get(idG);
            int hActual = mapG.getOrDefault(idA, 0);
            if (h > hActual) mapG.put(idA, h);
        }

        Map<Long, Map<String, Object>> res = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : mapaGrados.entrySet()) {
            Long idG = entry.getKey();
            Map<Long, Integer> mapG = entry.getValue();
            int totalH = mapG.values().stream().mapToInt(Integer::intValue).sum();
            res.put(idG, Map.of("totalHoras", totalH, "cantMaterias", mapG.size()));
        }

        return res;
    }

    @Transactional
    public void agregar(MallaRequest req) {
        String checkSql = """
                SELECT COUNT(*) FROM sga_principal.malla_curricular
                WHERE id_grado = :idGrado AND id_asignatura = :idAsignatura AND id_ano_lectivo = :idAnoLectivo
                """;
        Integer count = jdbc.queryForObject(checkSql,
                new MapSqlParameterSource()
                        .addValue("idGrado", req.idGrado())
                        .addValue("idAsignatura", req.idAsignatura())
                        .addValue("idAnoLectivo", req.idAnoLectivo()),
                Integer.class);

        if (count != null && count > 0) {
            throw ApiException.conflict("Esa asignatura ya está registrada en la malla del grado");
        }

        String insertSql = """
                INSERT INTO sga_principal.malla_curricular (id_grado, id_asignatura, id_ano_lectivo, horas_semana, dias_semana, duracion, activo, fecha_creacion)
                VALUES (:idGrado, :idAsignatura, :idAnoLectivo, :horasSemana, :diasSemana, :duracion, true, NOW())
                """;
        jdbc.update(insertSql,
                new MapSqlParameterSource()
                        .addValue("idGrado", req.idGrado())
                        .addValue("idAsignatura", req.idAsignatura())
                        .addValue("idAnoLectivo", req.idAnoLectivo())
                        .addValue("horasSemana", req.horasSemana())
                        .addValue("diasSemana", req.diasSemana())
                        .addValue("duracion", req.duracion()));
    }

    @Transactional
    public void actualizarHoras(long idMalla, short horasSemana) {
        int updated = jdbc.update(
                "UPDATE sga_principal.malla_curricular SET horas_semana = :horas WHERE id_malla = :id",
                new MapSqlParameterSource().addValue("id", idMalla).addValue("horas", horasSemana));
        if (updated == 0) {
            throw ApiException.notFound("Registro de malla no encontrado");
        }
    }

    @Transactional
    public void actualizarHorasGrado(ActualizarHorasGradoRequest req) {
        if (req.cambios() == null || req.cambios().isEmpty()) return;

        for (Map<String, Object> cambio : req.cambios()) {
            if (cambio.get("idAsignatura") == null || cambio.get("horasSemana") == null) continue;
            Long idAsig = Long.valueOf(cambio.get("idAsignatura").toString());
            int nuevasHoras = Integer.parseInt(cambio.get("horasSemana").toString());

            String findMalla = """
                    SELECT id_malla FROM sga_principal.malla_curricular
                    WHERE id_grado = :idGrado AND id_asignatura = :idAsig AND id_ano_lectivo = :idAno
                    """;
            List<Long> mallas = jdbc.query(findMalla,
                    new MapSqlParameterSource()
                            .addValue("idGrado", req.idGrado())
                            .addValue("idAsig", idAsig)
                            .addValue("idAno", req.idAnoLectivo()),
                    (rs, n) -> rs.getLong("id_malla"));

            if (!mallas.isEmpty()) {
                jdbc.update("UPDATE sga_principal.malla_curricular SET horas_semana = :horas WHERE id_malla = :id",
                        new MapSqlParameterSource().addValue("id", mallas.get(0)).addValue("horas", nuevasHoras));
            } else {
                jdbc.update("""
                        INSERT INTO sga_principal.malla_curricular (id_grado, id_asignatura, id_ano_lectivo, horas_semana, activo, fecha_creacion)
                        VALUES (:idGrado, :idAsig, :idAno, :horas, true, NOW())
                        """,
                        new MapSqlParameterSource()
                                .addValue("idGrado", req.idGrado())
                                .addValue("idAsig", idAsig)
                                .addValue("idAno", req.idAnoLectivo())
                                .addValue("horas", nuevasHoras));
            }

            jdbc.update("""
                    UPDATE sga_principal.asignaciones
                    SET horas_semanales = :horas
                    WHERE id_grado = :idGrado AND id_asignatura = :idAsig AND id_ano_lectivo = :idAno
                    """,
                    new MapSqlParameterSource()
                            .addValue("idGrado", req.idGrado())
                            .addValue("idAsig", idAsig)
                            .addValue("idAno", req.idAnoLectivo())
                            .addValue("horas", nuevasHoras));
        }
    }

    @Transactional
    public void eliminar(long idMalla) {
        int deleted = jdbc.update("DELETE FROM sga_principal.malla_curricular WHERE id_malla = :id",
                new MapSqlParameterSource("id", idMalla));
        if (deleted == 0) {
            throw ApiException.notFound("Registro de malla no encontrado");
        }
    }
}
