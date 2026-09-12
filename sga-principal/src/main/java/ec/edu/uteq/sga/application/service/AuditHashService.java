package ec.edu.uteq.sga.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * Servicio centralizado para el calculo del hash criptografico SHA-256 encadenado
 * para la bitacora de auditoria (blockchain-style).
 * Garantiza coincidencia canonica con la especificacion ADR-007 y los verificadores del cluster.
 */
@Service
public class AuditHashService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final ObjectMapper mapper;

    public AuditHashService() {
        this.mapper = new ObjectMapper();
        this.mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
    }

    /**
     * Genera la representación JSON canónica con claves ordenadas lexicográficamente
     * y sin espacios adicionales entre separadores.
     */
    public String jsonCanonico(Map<String, Object> contenido) {
        if (contenido == null) {
            return "{}";
        }
        try {
            // Asegurar ordenamiento de claves en mapas anidados
            Map<String, Object> ordenado = new TreeMap<>(contenido);
            return mapper.writeValueAsString(ordenado);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Error al serializar payload canonico de auditoria", e);
        }
    }

    /**
     * Calcula H_k = SHA-256(H_{k-1} + JSON_canonico(contenido)).
     */
    public String calcularHash(String hashAnterior, Map<String, Object> contenido) {
        String previo = (hashAnterior == null || hashAnterior.isBlank()) ? GENESIS_HASH : hashAnterior;
        String canonico = jsonCanonico(contenido);
        String material = previo + canonico;
        return sha256Hex(material);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", e);
        }
    }
}
