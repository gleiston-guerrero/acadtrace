package ec.uteq.sga.secretaria.service;

import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.RepresentanteRequest;
import ec.uteq.sga.secretaria.security.CryptoService;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * CRUD de representantes sobre sga_secretaria.representantes (mismo esquema
 * propio que Estudiante/Matricula: la FK sga_secretaria.estudiantes.id_representante
 * apunta a esta tabla, no a sga_principal.representantes). direccion/telefono_principal
 * se cifran igual que en EstudianteService, ya que son datos de contacto de
 * familiares de menores de edad.
 */
@Service
public class RepresentanteService {

    private static final List<String> CAMPOS_CIFRADOS = List.of("direccion", "telefono_principal");

    private final NamedParameterJdbcTemplate jdbc;
    private final CryptoService crypto;

    public RepresentanteService(NamedParameterJdbcTemplate jdbc, CryptoService crypto) {
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

    public List<Map<String, Object>> listarTodos(String search) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        String whereExtra = "";
        if (search != null && !search.isBlank()) {
            params.addValue("like", "%" + search + "%");
            whereExtra = "WHERE nombres ILIKE :like OR apellidos ILIKE :like OR cedula ILIKE :like";
        }
        String sql = """
                SELECT id_representante, cedula, nombres, apellidos, parentesco,
                       telefono_principal, telefono_alt, correo, direccion
                FROM sga_secretaria.representantes
                %s
                ORDER BY apellidos, nombres
                """.formatted(whereExtra);
        List<Map<String, Object>> data = jdbc.query(sql, params, GenericRowMapper.INSTANCE);
        data.forEach(this::descifrarFila);
        return data;
    }

    public Map<String, Object> obtenerPorId(long id) {
        List<Map<String, Object>> rows = jdbc.query(
                "SELECT * FROM sga_secretaria.representantes WHERE id_representante = :id",
                new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (rows.isEmpty()) throw ApiException.notFound("Representante no encontrado");
        return descifrarFila(rows.get(0));
    }

    public Map<String, Object> crear(RepresentanteRequest dto) {
        String cedula = blankToNull(dto.cedula());
        if (cedula != null) {
            List<Long> dup = jdbc.query(
                    "SELECT id_representante FROM sga_secretaria.representantes WHERE cedula = :cedula",
                    new MapSqlParameterSource("cedula", cedula), (rs, n) -> rs.getLong("id_representante"));
            if (!dup.isEmpty()) throw ApiException.conflict("Ya existe un representante con esa cédula");
        }

        MapSqlParameterSource params = camposExtendidos(dto)
                .addValue("cedula", cedula)
                .addValue("nombres", dto.nombres())
                .addValue("apellidos", dto.apellidos())
                .addValue("parentesco", blankToNull(dto.parentesco()))
                .addValue("telefonoPrincipal", crypto.encrypt(dto.telefono_principal()))
                .addValue("telefonoAlt", blankToNull(dto.telefono_alt()))
                .addValue("correo", blankToNull(dto.correo()))
                .addValue("direccion", crypto.encrypt(blankToNull(dto.direccion())));

        String sql = """
                INSERT INTO sga_secretaria.representantes
                  (cedula, nombres, apellidos, parentesco, telefono_principal, telefono_alt, correo, direccion,
                   fecha_nacimiento, genero, estado_civil, nacionalidad, ocupacion, lugar_trabajo, telefono_trabajo,
                   cargo, nivel_instruccion, ingreso_mensual, convive_con_estudiante,
                   contacto_emergencia_nombre, contacto_emergencia_telefono, observaciones)
                VALUES (:cedula, :nombres, :apellidos, :parentesco, :telefonoPrincipal, :telefonoAlt, :correo, :direccion,
                   :fechaNacimiento, :genero, :estadoCivil, :nacionalidad, :ocupacion, :lugarTrabajo, :telefonoTrabajo,
                   :cargo, :nivelInstruccion, :ingresoMensual, :conviveConEstudiante,
                   :contactoEmergenciaNombre, :contactoEmergenciaTelefono, :observaciones)
                RETURNING *
                """;
        return descifrarFila(jdbc.query(sql, params, GenericRowMapper.INSTANCE).get(0));
    }

    public Map<String, Object> actualizar(long id, RepresentanteRequest dto) {
        obtenerPorId(id);

        MapSqlParameterSource params = camposExtendidos(dto)
                .addValue("cedula", blankToNull(dto.cedula()))
                .addValue("nombres", blankToNull(dto.nombres()))
                .addValue("apellidos", blankToNull(dto.apellidos()))
                .addValue("parentesco", blankToNull(dto.parentesco()))
                .addValue("telefonoPrincipal", crypto.encrypt(blankToNull(dto.telefono_principal())))
                .addValue("telefonoAlt", blankToNull(dto.telefono_alt()))
                .addValue("correo", blankToNull(dto.correo()))
                .addValue("direccion", crypto.encrypt(blankToNull(dto.direccion())))
                .addValue("id", id);

        String sql = """
                UPDATE sga_secretaria.representantes SET
                  cedula                        = COALESCE(:cedula, cedula),
                  nombres                       = COALESCE(:nombres, nombres),
                  apellidos                     = COALESCE(:apellidos, apellidos),
                  parentesco                    = COALESCE(:parentesco, parentesco),
                  telefono_principal            = COALESCE(:telefonoPrincipal, telefono_principal),
                  telefono_alt                  = COALESCE(:telefonoAlt, telefono_alt),
                  correo                        = COALESCE(:correo, correo),
                  direccion                     = COALESCE(:direccion, direccion),
                  fecha_nacimiento              = :fechaNacimiento,
                  genero                        = :genero,
                  estado_civil                  = :estadoCivil,
                  nacionalidad                  = :nacionalidad,
                  ocupacion                     = :ocupacion,
                  lugar_trabajo                 = :lugarTrabajo,
                  telefono_trabajo              = :telefonoTrabajo,
                  cargo                         = :cargo,
                  nivel_instruccion             = :nivelInstruccion,
                  ingreso_mensual               = :ingresoMensual,
                  convive_con_estudiante        = :conviveConEstudiante,
                  contacto_emergencia_nombre    = :contactoEmergenciaNombre,
                  contacto_emergencia_telefono  = :contactoEmergenciaTelefono,
                  observaciones                 = :observaciones
                WHERE id_representante = :id
                RETURNING *
                """;
        return descifrarFila(jdbc.query(sql, params, GenericRowMapper.INSTANCE).get(0));
    }

    private MapSqlParameterSource camposExtendidos(RepresentanteRequest dto) {
        return new MapSqlParameterSource()
                .addValue("fechaNacimiento", blankToNull(dto.fecha_nacimiento()))
                .addValue("genero", blankToNull(dto.genero()))
                .addValue("estadoCivil", blankToNull(dto.estado_civil()))
                .addValue("nacionalidad", blankToNull(dto.nacionalidad()))
                .addValue("ocupacion", blankToNull(dto.ocupacion()))
                .addValue("lugarTrabajo", blankToNull(dto.lugar_trabajo()))
                .addValue("telefonoTrabajo", blankToNull(dto.telefono_trabajo()))
                .addValue("cargo", blankToNull(dto.cargo()))
                .addValue("nivelInstruccion", blankToNull(dto.nivel_instruccion()))
                .addValue("ingresoMensual", dto.ingreso_mensual())
                .addValue("conviveConEstudiante", dto.convive_con_estudiante())
                .addValue("contactoEmergenciaNombre", blankToNull(dto.contacto_emergencia_nombre()))
                .addValue("contactoEmergenciaTelefono", blankToNull(dto.contacto_emergencia_telefono()))
                .addValue("observaciones", blankToNull(dto.observaciones()));
    }

    /** Estudiantes a cargo de este representante (FK sga_secretaria.estudiantes.id_representante). */
    public List<Map<String, Object>> listarEstudiantes(long idRepresentante) {
        obtenerPorId(idRepresentante);
        String sql = """
                SELECT id_estudiante, cedula, codigo_estudiante, nombres, apellidos,
                       estado, foto_url
                FROM sga_secretaria.estudiantes
                WHERE id_representante = :id
                ORDER BY apellidos, nombres
                """;
        return jdbc.query(sql, new MapSqlParameterSource("id", idRepresentante), GenericRowMapper.INSTANCE);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
