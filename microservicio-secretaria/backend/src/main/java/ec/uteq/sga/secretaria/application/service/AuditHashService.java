package ec.uteq.sga.secretaria.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

@Service
public class AuditHashService {

    public static final String GENESIS_HASH =
            "0000000000000000000000000000000000000000000000000000000000000000";

    private static final Set<String> SECRET_KEYS = Set.of(
            "authorization",
            "jwt",
            "password",
            "contrasena",
            "contrase\u00f1a",
            "contrase?a",
            "internal_token",
            "token",
            "secret"
    );

    private final ObjectMapper mapper;

    public AuditHashService() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(
                SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                true
        );
    }

    public String jsonCanonico(Map<String, Object> contenido) {
        if (contenido == null) {
            return "{}";
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> normalizado =
                    (Map<String, Object>) normalizar(contenido);

            return mapper.writeValueAsString(normalizado);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Error al serializar payload canonico de auditoria",
                    e
            );
        }
    }

    public Map<String, Object> contenidoEvento(
            String tipoEvento,
            String entidad,
            Object entidadId,
            String operacion,
            Object actorId,
            String timestamp,
            Map<String, Object> payload,
            String modo,
            long relojLamport,
            Map<String, Long> relojVectorial,
            String estadoReconciliacion
    ) {
        Map<String, Object> contenido = new TreeMap<>();

        contenido.put("actor_id", actorId);
        contenido.put("entidad", entidad);
        contenido.put(
                "entidad_id",
                entidadId == null ? "None" : String.valueOf(entidadId)
        );
        contenido.put(
                "estado_reconciliacion",
                estadoReconciliacion
        );
        contenido.put("modo", modo);
        contenido.put("operacion", operacion);
        contenido.put(
                "payload",
                normalizar(payload != null ? payload : Map.of())
        );
        contenido.put("reloj_lamport", relojLamport);
        contenido.put(
                "reloj_vectorial",
                normalizar(relojVectorial)
        );
        contenido.put("timestamp", timestamp);
        contenido.put("tipo_evento", tipoEvento);

        return contenido;
    }

    public String calcularHash(
            String hashAnterior,
            Map<String, Object> contenido
    ) {
        String previo =
                hashAnterior == null || hashAnterior.isBlank()
                        ? GENESIS_HASH
                        : hashAnterior;

        return sha256Hex(previo + jsonCanonico(contenido));
    }

    public Map<String, Long> parseVector(String json) {
        Map<String, Long> vector = new TreeMap<>();

        if (json == null || json.isBlank()) {
            return vector;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw =
                    mapper.readValue(json, Map.class);

            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Object value = entry.getValue();

                if (value instanceof Number numero) {
                    vector.put(
                            entry.getKey(),
                            numero.longValue()
                    );
                }
            }

            return vector;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "vector_reloj persistido no es JSON valido",
                    e
            );
        }
    }

    public String vectorJson(Map<String, Long> vector) {
        try {
            return mapper.writeValueAsString(
                    new TreeMap<>(vector)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar vector_reloj",
                    e
            );
        }
    }

    private Object normalizar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> resultado = new TreeMap<>();

            for (Map.Entry<?, ?> entry : mapa.entrySet()) {
                String clave = String.valueOf(entry.getKey());

                if (SECRET_KEYS.contains(clave.toLowerCase())) {
                    continue;
                }

                resultado.put(
                        clave,
                        normalizar(entry.getValue())
                );
            }

            return resultado;
        }

        if (valor instanceof Collection<?> coleccion) {
            ArrayList<Object> resultado = new ArrayList<>();

            for (Object elemento : coleccion) {
                resultado.add(normalizar(elemento));
            }

            return resultado;
        }

        if (valor instanceof java.time.temporal.TemporalAccessor) {
            return valor.toString();
        }

        if (valor instanceof java.util.Date d) {
            return d.toInstant().toString();
        }

        if (valor instanceof java.math.BigDecimal) {
            return String.valueOf(valor);
        }

        return valor;
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] bytes = digest.digest(
                    input.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder sb =
                    new StringBuilder(bytes.length * 2);

            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 no disponible",
                    e
            );
        }
    }
}
