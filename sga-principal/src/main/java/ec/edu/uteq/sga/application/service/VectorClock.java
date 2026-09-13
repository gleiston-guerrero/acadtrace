package ec.edu.uteq.sga.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reloj vectorial para seguimiento de causalidad y ordenamiento parcial
 * en entornos concurrentes y distribuidos (sga-principal, docente, secretaria).
 * Conforme con ADR-007 (ISO/IEC 25010 - Integridad Causal).
 */
@Component
public class VectorClock {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final ConcurrentHashMap<String, AtomicLong> clock = new ConcurrentHashMap<>();

    public VectorClock() {
        // Inicializar nodos reconocidos del cluster
        clock.put("principal", new AtomicLong(0));
        clock.put("docente", new AtomicLong(0));
        clock.put("secretaria", new AtomicLong(0));
    }

    public synchronized Map<String, Long> increment(String node) {
        clock.computeIfAbsent(node, k -> new AtomicLong(0)).incrementAndGet();
        return current();
    }

    public synchronized Map<String, Long> merge(Map<String, Long> remote) {
        if (remote != null) {
            remote.forEach((node, val) -> {
                if (val != null) {
                    clock.compute(node, (k, currentVal) -> {
                        long current = (currentVal == null) ? 0L : currentVal.get();
                        return new AtomicLong(Math.max(current, val));
                    });
                }
            });
        }
        return current();
    }

    /**
     * Recupera el ultimo vector confirmado en BD antes de generar
     * un nuevo evento. Permite conservar causalidad tras reinicios
     * y entre distintas instancias del servicio.
     */
    public synchronized Map<String, Long> mergeJson(String json) {
        if (json == null || json.isBlank()) {
            return current();
        }
        try {
            Map<String, Long> remote = MAPPER.readValue(
                    json,
                    new TypeReference<Map<String, Long>>() {}
            );
            return merge(remote);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "vector_reloj persistido no es JSON valido",
                    e
            );
        }
    }

    public Map<String, Long> current() {
        Map<String, Long> snapshot = new TreeMap<>();
        clock.forEach((k, v) -> snapshot.put(k, v.get()));
        return snapshot;
    }

    public String toJson() {
        try {
            return MAPPER.writeValueAsString(current());
        } catch (JsonProcessingException e) {
            return "{\"principal\":" + clock.getOrDefault("principal", new AtomicLong(0)).get() + "}";
        }
    }
}
