package ec.edu.uteq.sga.application.service;

import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Reloj logico de Lamport para ordenar causalmente eventos de auditoria y calificaciones
 * en sga-principal sin depender del reloj de pared (NTP drift).
 * Regla: local = max(local, remoto) + 1.
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

    public long seed(long value) {
        return counter.updateAndGet(local -> Math.max(local, value));
    }

    public long current() {
        return counter.get();
    }
}
