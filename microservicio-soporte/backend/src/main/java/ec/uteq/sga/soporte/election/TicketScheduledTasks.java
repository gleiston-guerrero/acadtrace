package ec.uteq.sga.soporte.election;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Tareas de fondo del microservicio de soporte. Cuando corran varias
 * replicas (deploy.replicas > 1), TODAS las replicas tienen este @Scheduled
 * activo, pero cada corrida se corta al toque si isLeader() da false -- asi
 * solo la replica lider (elegida via LeaderElectionService/etcd) hace el
 * trabajo real, evitando que 2 replicas cierren o notifiquen el mismo
 * ticket dos veces.
 */
@Component
public class TicketScheduledTasks {

    private static final Logger log = LoggerFactory.getLogger(TicketScheduledTasks.class);

    private final LeaderElectionService leaderElection;
    private final NamedParameterJdbcTemplate jdbc;

    @Value("${scheduling.cierre-automatico.dias-inactividad:30}")
    private int diasInactividad;

    public TicketScheduledTasks(LeaderElectionService leaderElection, NamedParameterJdbcTemplate jdbc) {
        this.leaderElection = leaderElection;
        this.jdbc = jdbc;
    }

    /**
     * Cierra automaticamente tickets ABIERTO/EN_PROCESO sin actividad
     * (creacion) hace mas de diasInactividad dias. Deja registrado el
     * motivo en solucion_aplicada para que quede trazable en el historial.
     */
    @Scheduled(fixedDelayString = "${scheduling.cierre-automatico.fixed-delay-ms:3600000}")
    public void cerrarTicketsInactivos() {
        if (!leaderElection.isLeader()) {
            return; // esta replica no es la lider: no hace nada
        }

        List<Map<String, Object>> cerrados = jdbc.queryForList(
                """
                UPDATE sga_soporte.tickets
                   SET estado = 'CERRADO',
                       fecha_resolucion = NOW(),
                       solucion_aplicada = COALESCE(solucion_aplicada,
                           'Cerrado automaticamente por inactividad (' || :dias || ' dias sin novedad).')
                 WHERE estado IN ('ABIERTO', 'EN_PROCESO')
                   AND fecha_creacion < NOW() - (:dias || ' days')::interval
                RETURNING numero_ticket
                """,
                new MapSqlParameterSource("dias", diasInactividad)
        );

        if (!cerrados.isEmpty()) {
            log.info("[scheduled:{}] {} ticket(s) cerrados automaticamente por inactividad ({} dias): {}",
                    leaderElection.instanceId(), cerrados.size(), diasInactividad, cerrados);
        }
    }

    /**
     * Recordatorios de tickets abiertos con alta prioridad. El proyecto no
     * tiene infraestructura de correo configurada todavia (sin SMTP en
     * application.properties), asi que por ahora esto solo deja constancia
     * en el log de que tickets necesitarian un recordatorio -- el envio
     * real de correo es una integracion aparte, pendiente de credenciales
     * SMTP.
     */
    @Scheduled(fixedDelayString = "${scheduling.recordatorios.fixed-delay-ms:3600000}")
    public void enviarRecordatorios() {
        if (!leaderElection.isLeader()) {
            return;
        }

        List<Map<String, Object>> pendientes = jdbc.queryForList(
                """
                SELECT numero_ticket, creado_por, asignado_a, prioridad
                  FROM sga_soporte.tickets
                 WHERE estado IN ('ABIERTO', 'EN_PROCESO')
                   AND prioridad IN ('ALTO', 'CRITICO')
                """,
                new MapSqlParameterSource()
        );

        if (!pendientes.isEmpty()) {
            log.info("[scheduled:{}] {} ticket(s) de prioridad alta/critica pendientes de recordatorio: {}",
                    leaderElection.instanceId(), pendientes.size(), pendientes);
        }
    }
}
