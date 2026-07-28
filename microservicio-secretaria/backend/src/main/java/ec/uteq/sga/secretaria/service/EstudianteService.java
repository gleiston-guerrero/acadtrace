package ec.uteq.sga.secretaria.service;

import ec.edu.uteq.sga.grpc.principal.EstudianteProto;
import ec.edu.uteq.sga.grpc.principal.GuardarEstudianteRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesRequest;
import ec.edu.uteq.sga.grpc.principal.ListarEstudiantesResponse;
import ec.uteq.sga.secretaria.common.ApiException;
import ec.uteq.sga.secretaria.common.PageResult;
import ec.uteq.sga.secretaria.common.jdbc.GenericRowMapper;
import ec.uteq.sga.secretaria.dto.EstudianteRequest;
import ec.uteq.sga.secretaria.grpc.PrincipalGrpcClient;
import ec.uteq.sga.secretaria.security.CryptoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class EstudianteService {

    private static final Logger log = LoggerFactory.getLogger(EstudianteService.class);

    /**
     * Columnas cifradas con AES-256-GCM (CryptoService): las propias de
     * estudiantes (direccion/telefono/tipo_discapacidad) mas los alias de las
     * columnas ya cifradas por RepresentanteService (rep_telefono) y
     * FichaEstudianteService (detalle_enfermedad, medicacion_permanente,
     * alergias) que este servicio solo lee por JOIN. cedula y correo quedan
     * en claro porque se usan en busquedas/uniqueness (ILIKE, WHERE =).
     */
    private static final List<String> CAMPOS_CIFRADOS = List.of(
            "direccion", "telefono", "tipo_discapacidad",
            "rep_telefono", "detalle_enfermedad", "medicacion_permanente", "alergias");

    private final NamedParameterJdbcTemplate jdbc;
    private final CryptoService crypto;
    private final PrincipalGrpcClient principalGrpcClient;

    public EstudianteService(NamedParameterJdbcTemplate jdbc, CryptoService crypto, PrincipalGrpcClient principalGrpcClient) {
        this.jdbc = jdbc;
        this.crypto = crypto;
        this.principalGrpcClient = principalGrpcClient;
    }

    /**
     * Descifra los campos sensibles de una fila. Tolera valores en texto plano
     * (estudiantes existentes de antes de activar el cifrado, o filas
     * escritas por sga-principal, que hoy no cifra) devolviendolos tal cual en
     * vez de fallar, para no romper la lectura de datos historicos.
     */
    private Map<String, Object> descifrarFila(Map<String, Object> row) {
        for (String campo : CAMPOS_CIFRADOS) {
            if (row.containsKey(campo)) {
                String valor = (String) row.get(campo);
                if (valor != null) {
                    try {
                        row.put(campo, crypto.decrypt(valor));
                    } catch (RuntimeException e) {
                        log.warn("No se pudo descifrar '{}' (probable dato en texto plano previo al cifrado); " +
                                "se devuelve sin cambios", campo);
                    }
                }
            }
        }
        return row;
    }

    /**
     * estudiantes.estado es varchar en la base ('ACTIVO'/'ACTIVA', con datos
     * legados inconsistentes en genero), no boolean como asumia el codigo
     * original. Se normaliza a boolean en la respuesta para no romper el
     * contrato con el frontend (Usuarios.jsx y Estudiantes.jsx ya esperan
     * estado true/false).
     */
    private Map<String, Object> normalizarEstado(Map<String, Object> row) {
        if (row.containsKey("estado")) {
            row.put("estado", esActivo(row.get("estado")));
        }
        return row;
    }

    private static boolean esActivo(Object estado) {
        if (estado instanceof Boolean b) return b;
        if (estado == null) return false;
        String texto = estado.toString().trim().toUpperCase();
        return texto.equals("ACTIVO") || texto.equals("ACTIVA");
    }

    private Map<String, Object> posprocesar(Map<String, Object> row) {
        return descifrarFila(normalizarEstado(row));
    }

    public PageResult<Map<String, Object>> listarTodos(String search, int page, int limit) {
        ListarEstudiantesRequest request = ListarEstudiantesRequest.newBuilder()
                .setQ(orEmpty(search))
                .setPage(page)
                .setLimit(limit)
                .build();
        ListarEstudiantesResponse response = principalGrpcClient.listarEstudiantes(request);

        List<Map<String, Object>> data = response.getEstudiantesList().stream()
                .map(this::fromProto)
                .map(this::posprocesar)
                .toList();

        return PageResult.of(data, response.getTotal(), page, limit);
    }

    public Map<String, Object> obtenerPorId(long id) {
        EstudianteProto proto = principalGrpcClient.obtenerEstudiante(id);
        Map<String, Object> row = posprocesar(fromProto(proto));

        // Ficha médica: FichaEstudiante todavía no migró a gRPC (fuera de alcance de esta migración),
        // se conserva como enriquecimiento de solo lectura vía SQL directo.
        List<Map<String, Object>> ficha = jdbc.query(
                """
                SELECT tipo_sangre, alergias, medicacion_permanente,
                       enfermedad_catastrofica, detalle_enfermedad,
                       contacto_emergencia, telefono_emergencia
                FROM sga_secretaria.fichas_estudiante WHERE id_estudiante = :id
                """,
                new MapSqlParameterSource("id", id), GenericRowMapper.INSTANCE);
        if (!ficha.isEmpty()) row.putAll(descifrarFila(ficha.get(0)));

        return row;
    }

    public Map<String, Object> crear(EstudianteRequest dto, String username) {
        String cedula = blankToNull(dto.cedula());
        if (cedula != null) {
            List<Map<String, Object>> dup = jdbc.query(
                    "SELECT id_estudiante FROM sga_secretaria.estudiantes WHERE cedula = :cedula",
                    new MapSqlParameterSource("cedula", cedula), GenericRowMapper.INSTANCE);
            if (!dup.isEmpty()) throw ApiException.conflict("Ya existe un estudiante con esa cédula");
        }

        Long creadoPor = resolverCreadoPor(username);
        String codigo = generarSiguienteCodigo();

        GuardarEstudianteRequest request = GuardarEstudianteRequest.newBuilder()
                .setCedula(orEmpty(cedula))
                .setNombres(orEmpty(dto.nombres()))
                .setApellidos(orEmpty(dto.apellidos()))
                .setFechaNacimiento(orEmpty(dto.fecha_nacimiento()))
                .setGenero(orEmpty(blankToNull(dto.genero())))
                .setCorreo(orEmpty(dto.correo()))
                .setDireccion(orEmpty(crypto.encrypt(blankToNull(dto.direccion()))))
                .setTelefono(orEmpty(crypto.encrypt(blankToNull(dto.telefono()))))
                .setDiscapacidad(dto.discapacidad() != null && dto.discapacidad())
                .setTipoDiscapacidad(orEmpty(crypto.encrypt(blankToNull(dto.tipo_discapacidad()))))
                .setPorcentajeDisc(dto.porcentaje_disc() != null ? dto.porcentaje_disc() : 0)
                .setIdRepresentante(dto.id_representante() != null ? dto.id_representante() : 0)
                .setIdUsuarioCreador(creadoPor != null ? creadoPor : 0)
                .setCodigoEstudiante(codigo)
                .setNacionalidad(orEmpty(dto.nacionalidad()))
                .setEtnia(orEmpty(dto.etnia()))
                .setLugarNacimiento(orEmpty(dto.lugar_nacimiento()))
                .setViveCon(orEmpty(dto.vive_con()))
                .setNumerosHermanos(dto.numeros_hermanos() != null ? dto.numeros_hermanos() : 0)
                .setBeneficioSocial(dto.beneficio_social() != null && dto.beneficio_social())
                .setCarnetConadis(orEmpty(dto.carnet_conadis()))
                .setFotoUrl(orEmpty(dto.foto_url()))
                .build();

        EstudianteProto creado = principalGrpcClient.crearEstudiante(request);
        return posprocesar(fromProto(creado));
    }

    public Map<String, Object> actualizar(long id, EstudianteRequest dto) {
        Map<String, Object> actual = obtenerPorId(id); // ya descifrado (posprocesar) — sirve de base para los campos no enviados

        String cedula = blankToNull(dto.cedula()) != null ? dto.cedula() : (String) actual.get("cedula");
        String nombres = blankToNull(dto.nombres()) != null ? dto.nombres() : (String) actual.get("nombres");
        String apellidos = blankToNull(dto.apellidos()) != null ? dto.apellidos() : (String) actual.get("apellidos");
        String fechaNacimiento = blankToNull(dto.fecha_nacimiento()) != null
                ? dto.fecha_nacimiento()
                : (actual.get("fecha_nacimiento") != null ? actual.get("fecha_nacimiento").toString() : null);
        String genero = blankToNull(dto.genero()) != null ? dto.genero() : (String) actual.get("genero");
        String correo = blankToNull(dto.correo()) != null ? dto.correo() : (String) actual.get("correo");
        String direccion = blankToNull(dto.direccion()) != null ? dto.direccion() : (String) actual.get("direccion");
        String telefono = blankToNull(dto.telefono()) != null ? dto.telefono() : (String) actual.get("telefono");
        String tipoDiscapacidad = blankToNull(dto.tipo_discapacidad()) != null
                ? dto.tipo_discapacidad() : (String) actual.get("tipo_discapacidad");
        boolean discapacidad = dto.discapacidad() != null ? dto.discapacidad() : Boolean.TRUE.equals(actual.get("discapacidad"));
        Integer porcentajeDisc = dto.porcentaje_disc() != null
                ? dto.porcentaje_disc()
                : (actual.get("porcentaje_disc") != null ? ((Number) actual.get("porcentaje_disc")).intValue() : null);
        Long idRepresentante = dto.id_representante() != null
                ? dto.id_representante()
                : (actual.get("id_representante") != null ? ((Number) actual.get("id_representante")).longValue() : null);
        String nacionalidad = blankToNull(dto.nacionalidad()) != null ? dto.nacionalidad() : (String) actual.get("nacionalidad");
        String etnia = blankToNull(dto.etnia()) != null ? dto.etnia() : (String) actual.get("etnia");
        String lugarNacimiento = blankToNull(dto.lugar_nacimiento()) != null ? dto.lugar_nacimiento() : (String) actual.get("lugar_nacimiento");
        String viveCon = blankToNull(dto.vive_con()) != null ? dto.vive_con() : (String) actual.get("vive_con");
        Integer numerosHermanos = dto.numeros_hermanos() != null
                ? dto.numeros_hermanos()
                : (actual.get("numeros_hermanos") != null ? ((Number) actual.get("numeros_hermanos")).intValue() : null);
        boolean beneficioSocial = dto.beneficio_social() != null ? dto.beneficio_social() : Boolean.TRUE.equals(actual.get("beneficio_social"));
        String carnetConadis = blankToNull(dto.carnet_conadis()) != null ? dto.carnet_conadis() : (String) actual.get("carnet_conadis");
        String fotoUrl = blankToNull(dto.foto_url()) != null ? dto.foto_url() : (String) actual.get("foto_url");

        GuardarEstudianteRequest request = GuardarEstudianteRequest.newBuilder()
                .setIdEstudiante(id)
                .setCedula(orEmpty(cedula))
                .setNombres(orEmpty(nombres))
                .setApellidos(orEmpty(apellidos))
                .setFechaNacimiento(orEmpty(fechaNacimiento))
                .setGenero(orEmpty(genero))
                .setCorreo(orEmpty(correo))
                .setDireccion(orEmpty(crypto.encrypt(direccion)))
                .setTelefono(orEmpty(crypto.encrypt(telefono)))
                .setDiscapacidad(discapacidad)
                .setTipoDiscapacidad(orEmpty(crypto.encrypt(tipoDiscapacidad)))
                .setPorcentajeDisc(porcentajeDisc != null ? porcentajeDisc : 0)
                .setIdRepresentante(idRepresentante != null ? idRepresentante : 0)
                .setNacionalidad(orEmpty(nacionalidad))
                .setEtnia(orEmpty(etnia))
                .setLugarNacimiento(orEmpty(lugarNacimiento))
                .setViveCon(orEmpty(viveCon))
                .setNumerosHermanos(numerosHermanos != null ? numerosHermanos : 0)
                .setBeneficioSocial(beneficioSocial)
                .setCarnetConadis(orEmpty(carnetConadis))
                .setFotoUrl(orEmpty(fotoUrl))
                // codigo_estudiante e id_usuario_creador en blanco/0: ActualizarEstudiante no los toca (igual que el UPDATE original)
                .build();

        EstudianteProto actualizado = principalGrpcClient.actualizarEstudiante(request);
        return posprocesar(fromProto(actualizado));
    }

    private Long resolverCreadoPor(String username) {
        List<Long> creadorIds = jdbc.query(
                "SELECT id_usuario FROM sga_principal.usuarios WHERE username = :username",
                new MapSqlParameterSource("username", username),
                (rs, n) -> rs.getLong("id_usuario"));
        return creadorIds.isEmpty() ? null : creadorIds.get(0);
    }

    private String generarSiguienteCodigo() {
        List<String> ultimoCodigo = jdbc.query(
                "SELECT codigo_estudiante FROM sga_secretaria.estudiantes " +
                        "WHERE codigo_estudiante IS NOT NULL ORDER BY id_estudiante DESC LIMIT 1",
                (rs, n) -> rs.getString("codigo_estudiante"));
        String codigo = "EST-0001";
        if (!ultimoCodigo.isEmpty() && ultimoCodigo.get(0) != null) {
            String[] parts = ultimoCodigo.get(0).split("-");
            int num = 0;
            if (parts.length > 1) {
                try {
                    num = Integer.parseInt(parts[1]);
                } catch (NumberFormatException ignored) {
                    // deja num en 0, igual que parseInt(NaN) || 0 en Node
                }
            }
            codigo = "EST-%04d".formatted(num + 1);
        }
        return codigo;
    }

    /** Convierte la respuesta de sga-principal (ya persistida) al mismo shape de Map que devolvía RETURNING *. */
    private Map<String, Object> fromProto(EstudianteProto p) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id_estudiante", p.getIdEstudiante());
        row.put("cedula", blankToNull(p.getCedula()));
        row.put("codigo_estudiante", blankToNull(p.getCodigoEstudiante()));
        row.put("nombres", p.getNombres());
        row.put("apellidos", p.getApellidos());
        row.put("fecha_nacimiento", blankToNull(p.getFechaNacimiento()) != null ? LocalDate.parse(p.getFechaNacimiento()) : null);
        row.put("genero", blankToNull(p.getGenero()));
        row.put("direccion", blankToNull(p.getDireccion()));
        row.put("telefono", blankToNull(p.getTelefono()));
        row.put("correo", blankToNull(p.getCorreo()));
        row.put("discapacidad", p.getDiscapacidad());
        row.put("tipo_discapacidad", blankToNull(p.getTipoDiscapacidad()));
        row.put("porcentaje_disc", p.getPorcentajeDisc());
        row.put("id_representante", p.getIdRepresentante() > 0 ? p.getIdRepresentante() : null);
        row.put("estado", p.getEstado());
        row.put("rep_nombres", blankToNull(p.getRepNombres()));
        row.put("rep_apellidos", blankToNull(p.getRepApellidos()));
        row.put("rep_telefono", blankToNull(p.getRepTelefono()));
        row.put("parentesco", blankToNull(p.getRepParentesco()));
        row.put("nacionalidad", blankToNull(p.getNacionalidad()));
        row.put("etnia", blankToNull(p.getEtnia()));
        row.put("lugar_nacimiento", blankToNull(p.getLugarNacimiento()));
        row.put("vive_con", blankToNull(p.getViveCon()));
        row.put("numeros_hermanos", p.getNumerosHermanos());
        row.put("beneficio_social", p.getBeneficioSocial());
        row.put("carnet_conadis", blankToNull(p.getCarnetConadis()));
        row.put("foto_url", blankToNull(p.getFotoUrl()));
        return row;
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }

    public void cambiarEstado(long id, boolean estado) {
        principalGrpcClient.cambiarEstadoEstudiante(id, estado);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}
