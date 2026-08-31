package ec.edu.uteq.sga.infrastructure.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Módulo F: Métricas de Negocio personalizadas para Prometheus / Grafana.
 * Registra los 4 contadores obligatorios asignados al equipo BCEL.
 */
@Component
public class SgaBusinessMetrics {

    private final Counter notasRegistradasCounter;
    private final Counter notasModificadasCounter;
    private final Counter matriculasConfirmadasCounter;
    private final Counter notificacionesEnviadasCounter;
    private final Timer tiempoTransaccionTimer;
    private final AtomicInteger estudiantesActivosGauge = new AtomicInteger(344);

    public SgaBusinessMetrics(MeterRegistry registry) {
        // 1. Contador de notas registradas
        this.notasRegistradasCounter = Counter.builder("sga_notas_registradas_total")
                .description("Total de calificaciones asentadas en el sistema")
                .tag("servicio", "sga-principal")
                .register(registry);

        // 2. Contador de notas modificadas (auditoría)
        this.notasModificadasCounter = Counter.builder("sga_notas_modificadas_total")
                .description("Total de modificaciones de notas sujetas a trazabilidad criptográfica")
                .tag("servicio", "sga-principal")
                .register(registry);

        // 3. Contador de matrículas confirmadas
        this.matriculasConfirmadasCounter = Counter.builder("sga_matriculas_confirmadas_total")
                .description("Total de matrículas legalizadas en el período lectivo")
                .tag("servicio", "sga-principal")
                .register(registry);

        // 4. Contador de notificaciones enviadas a representantes
        this.notificacionesEnviadasCounter = Counter.builder("sga_notificaciones_enviadas_total")
                .description("Total de notificaciones push despachadas a representantes")
                .tag("servicio", "sga-principal")
                .register(registry);

        // 5. Timer para medir la latencia de transacciones de notas
        this.tiempoTransaccionTimer = Timer.builder("sga_transaccion_duracion_seconds")
                .description("Duración de las operaciones académicas transaccionales")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("servicio", "sga-principal")
                .register(registry);

        // 6. Gauge para medir estudiantes activos concurrentes
        Gauge.builder("sga_estudiantes_activos_total", estudiantesActivosGauge, AtomicInteger::get)
                .description("Cantidad de estudiantes con estado activo en el sistema")
                .tag("servicio", "sga-principal")
                .register(registry);
    }

    public void registrarNota() { notasRegistradasCounter.increment(); }
    public void modificarNota() { notasModificadasCounter.increment(); }
    public void confirmarMatricula() { matriculasConfirmadasCounter.increment(); }
    public void enviarNotificacion() { notificacionesEnviadasCounter.increment(); }
    public Timer getTimer() { return tiempoTransaccionTimer; }
    public void setEstudiantesActivos(int cantidad) { estudiantesActivosGauge.set(cantidad); }
}