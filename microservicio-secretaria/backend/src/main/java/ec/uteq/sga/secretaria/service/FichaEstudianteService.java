package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.FichaEstudianteRequest;
import ec.uteq.sga.secretaria.security.CryptoService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Ficha medica/de emergencia del estudiante (sga_secretaria.fichas_estudiante,
 * relacion 1 a 1 por id_estudiante, con FK hacia sga_secretaria.estudiantes).
 * detalle_enfermedad/medicacion_permanente/
 * alergias/direccion_referencia se cifran con CryptoService por ser datos de
 * salud sensibles de un menor de edad; enfermedad_catastrofica es una bandera
 * boolean y tipo_sangre/contacto_emergencia/telefono_emergencia quedan en
 * claro (necesarios en texto plano ante una emergencia).
 */
@Service
public class FichaEstudianteService {

    private static final List<String> CAMPOS_CIFRADOS =
            List.of("detalle_enfermedad", "medicacion_permanente", "alergias", "direccion_referencia");

    private final NamedParameterJdbcTemplate jdbc;
    private final CryptoService crypto;

    public FichaEstudianteService(NamedParameterJdbcTemplate jdbc, CryptoService crypto) {
        this.jdbc = jdbc;
        this.crypto = crypto;
    }

    private Map<String, Object> descifrarFila(Map<String, Object> row) {
        for (String campo : CAMPOS_CIFRADOS) {
            if (row.containsKey(campo)) {
                String valor = (String) row.get(campo);
                if (valor != null) {
                    try {
                        row.put(campo, crypto.decrypt(valor));
                    } catch (RuntimeException ignored) {
                        // dato en texto plano previo al cifrado: se devuelve tal cual
                    }
                }
            }
        }
        return row;
    }

    public Map<String, Object> obtenerPorEstudiante(long idEstudiante) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM sga_secretaria.fichas_estudiante WHERE id_estudiante = :id",
                new MapSqlParameterSource("id", idEstudiante), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("Este estudiante todavía no tiene ficha registrada");
        return descifrarFila(rows.get(0));
    }

    public Map<String, Object> guardar(long idEstudiante, FichaEstudianteRequest dto) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("idEstudiante", idEstudiante)
                .addValue("tipoSangre", blankToNull(dto.tipo_sangre()))
                .addValue("enfermedadCatastrofica", dto.enfermedad_catastrofica() != null && dto.enfermedad_catastrofica())
                .addValue("detalleEnfermedad", crypto.encrypt(blankToNull(dto.detalle_enfermedad())))
                .addValue("medicacionPermanente", crypto.encrypt(blankToNull(dto.medicacion_permanente())))
                .addValue("alergias", crypto.encrypt(blankToNull(dto.alergias())))
                .addValue("contactoEmergencia", blankToNull(dto.contacto_emergencia()))
                .addValue("telefonoEmergencia", blankToNull(dto.telefono_emergencia()))
                .addValue("direccionReferencia", crypto.encrypt(blankToNull(dto.direccion_referencia())));

        String sql = """
                INSERT INTO sga_secretaria.fichas_estudiante
                  (id_estudiante, tipo_sangre, enfermedad_catastrofica, detalle_enfermedad,
                   medicacion_permanente, alergias, contacto_emergencia, telefono_emergencia,
                   direccion_referencia, fecha_actualizacion)
                VALUES (:idEstudiante, :tipoSangre, :enfermedadCatastrofica, :detalleEnfermedad,
                        :medicacionPermanente, :alergias, :contactoEmergencia, :telefonoEmergencia,
                        :direccionReferencia, NOW())
                ON CONFLICT (id_estudiante) DO UPDATE SET
                  tipo_sangre             = EXCLUDED.tipo_sangre,
                  enfermedad_catastrofica = EXCLUDED.enfermedad_catastrofica,
                  detalle_enfermedad      = EXCLUDED.detalle_enfermedad,
                  medicacion_permanente   = EXCLUDED.medicacion_permanente,
                  alergias                = EXCLUDED.alergias,
                  contacto_emergencia     = EXCLUDED.contacto_emergencia,
                  telefono_emergencia     = EXCLUDED.telefono_emergencia,
                  direccion_referencia    = EXCLUDED.direccion_referencia,
                  fecha_actualizacion     = NOW()
                RETURNING *
                """;
        return descifrarFila(jdbc.query(sql, params, GenericRowMapper.INSTANCE).get(0));
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
