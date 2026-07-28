package ec.uteq.sga.secretaria.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Reloj logico de Lamport para ordenar eventos de historial_promocion sin
 * depender del reloj de pared, que puede desincronizarse entre Secretaria,
 * sga-principal y Supabase. Regla clasica: cada evento local incrementa el
 * contador; update(remoto) lo adelanta si llega un timestamp logico mayor
 * desde otro proceso: local = max(local, remoto) + 1.
 */
@Component
public class LamportClock {

    private final AtomicLong counter = new AtomicLong(0);

    public long tick() {
        return counter.incrementAndGet();
    }

    public long update(long remoteTimestamp) {
        return counter.updateAndGet(local -> Math.max(local, remoteTimestamp) + 1);
    }

    /** Adelanta el contador sin incrementar de mas; usado al arrancar el servicio para no repetir valores ya persistidos. */
    public long seed(long value) {
        return counter.updateAndGet(local -> Math.max(local, value));
    }

    public long current() {
        return counter.get();
    }
}
