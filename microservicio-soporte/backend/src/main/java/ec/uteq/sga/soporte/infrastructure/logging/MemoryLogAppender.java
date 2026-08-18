package ec.uteq.sga.soporte.infrastructure.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Appender de Logback que guarda en memoria (buffer circular) los ultimos
 * eventos ERROR/WARN del propio proceso, para que el Dashboard pueda
 * mostrarlos sin depender de "docker logs" por SSH. Registrado desde
 * logback-spring.xml.
 *
 * Es memoria pura, no persiste entre reinicios -- para eso ya existe el log
 * de auditoria de negocio (tabla en BD). Esto es solo para diagnostico
 * operativo rapido de excepciones tecnicas.
 */
public class MemoryLogAppender extends AppenderBase<ILoggingEvent> {

    private static final int CAPACIDAD_MAXIMA = 200;

    // Deque no es thread-safe por si solo; se sincroniza manualmente porque
    // Logback puede invocar append() desde varios hilos concurrentemente.
    private static final Deque<Map<String, Object>> BUFFER = new ArrayDeque<>(CAPACIDAD_MAXIMA);

    @Override
    protected void append(ILoggingEvent event) {
        if (event.getLevel().toInt() < ch.qos.logback.classic.Level.WARN.toInt()) {
            return; // solo nos interesan WARN y ERROR para el panel de fallos
        }

        Map<String, Object> entrada = new LinkedHashMap<>();
        entrada.put("timestamp", Instant.ofEpochMilli(event.getTimeStamp()).toString());
        entrada.put("nivel", event.getLevel().toString());
        entrada.put("logger", acortarLogger(event.getLoggerName()));
        entrada.put("mensaje", event.getFormattedMessage());
        if (event.getThrowableProxy() != null) {
            entrada.put("excepcion", event.getThrowableProxy().getClassName() + ": " + event.getThrowableProxy().getMessage());
        }

        synchronized (BUFFER) {
            if (BUFFER.size() >= CAPACIDAD_MAXIMA) {
                BUFFER.removeLast();
            }
            BUFFER.addFirst(entrada); // mas reciente primero
        }
    }

    /** com.foo.bar.MiClase -> MiClase, para no saturar la UI con paquetes completos. */
    private String acortarLogger(String nombreCompleto) {
        int i = nombreCompleto.lastIndexOf('.');
        return i >= 0 ? nombreCompleto.substring(i + 1) : nombreCompleto;
    }

    public static List<Map<String, Object>> ultimos(int limite) {
        synchronized (BUFFER) {
            return BUFFER.stream().limit(limite).collect(Collectors.toList());
        }
    }
}
