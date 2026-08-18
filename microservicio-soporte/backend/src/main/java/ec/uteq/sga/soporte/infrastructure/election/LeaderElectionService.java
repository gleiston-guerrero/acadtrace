package ec.uteq.sga.soporte.election;

import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.Election;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.election.LeaderKey;
import io.etcd.jetcd.election.LeaderResponse;
import io.etcd.jetcd.lease.LeaseKeepAliveResponse;
import io.etcd.jetcd.support.CloseableClient;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Eleccion de lider entre replicas de sga-soporte usando la Election API
 * nativa de etcd (io.etcd.jetcd.Election), no una implementacion casera de
 * Raft: etcd ya corre Raft internamente, esta clase solo es cliente de esa
 * API.
 *
 * Como funciona:
 *  1. Al arrancar, esta instancia pide un lease (TTL corto) y "compite" por
 *     el nombre de eleccion (etcd.election.name, por defecto /sga/leader)
 *     proponiendo su propio id como valor.
 *  2. campaign() no resuelve hasta que esta instancia efectivamente gana
 *     la eleccion: si ya hay un lider, se queda en cola (isLeader() = false)
 *     hasta que el lider actual se caiga (lease expira) o resigne.
 *  3. Mientras el proceso siga vivo, el lease se renueva solo (keepAlive);
 *     si el proceso muere o se corta la red, el lease expira solo (TTL
 *     segundos) y la siguiente replica en cola gana la eleccion.
 *  4. isLeader() es lo que deben consultar los @Scheduled que solo deben
 *     correr en una replica (ver TicketScheduledTasks).
 *
 * Verificacion en la demo: `docker exec etcd etcdctl get --prefix /sga/leader`
 */
@Service
public class LeaderElectionService {

    private static final Logger log = LoggerFactory.getLogger(LeaderElectionService.class);

    @Value("${etcd.endpoints}")
    private String endpoints;

    @Value("${etcd.election.name}")
    private String electionName;

    @Value("${etcd.lease.ttl-seconds}")
    private long leaseTtlSeconds;

    @Value("${app.instance.id:}")
    private String configuredInstanceId;

    private String instanceId;
    private Client client;
    private Lease leaseClient;
    private Election electionClient;
    private long leaseId;
    private CloseableClient keepAliveHandle;

    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    private final AtomicReference<LeaderKey> leaderKey = new AtomicReference<>();
    private final AtomicReference<String> currentLeaderId = new AtomicReference<>("desconocido");

    @PostConstruct
    public void iniciar() {
        instanceId = (configuredInstanceId == null || configuredInstanceId.isBlank())
                ? "soporte-" + UUID.randomUUID().toString().substring(0, 8)
                : configuredInstanceId;

        client = Client.builder().endpoints(endpoints.split(",")).build();
        leaseClient = client.getLeaseClient();
        electionClient = client.getElectionClient();

        ByteSequence election = ByteSequence.from(electionName, StandardCharsets.UTF_8);
        ByteSequence proposal = ByteSequence.from(instanceId, StandardCharsets.UTF_8);

        try {
            leaseId = leaseClient.grant(leaseTtlSeconds).get().getID();
        } catch (Exception e) {
            log.error("[election] No se pudo obtener un lease de etcd en {}: {}", endpoints, e.getMessage());
            return; // sin etcd disponible, la instancia sencillamente nunca sera lider
        }

        // Mantiene el lease vivo mientras el proceso siga arriba. Si el
        // proceso muere (kill -9, docker stop, crash), nadie vuelve a
        // llamar a esto y el lease expira solo tras leaseTtlSeconds.
        keepAliveHandle = leaseClient.keepAlive(leaseId, new StreamObserver<>() {
            @Override
            public void onNext(LeaseKeepAliveResponse response) {
                // nada que hacer: mientras sigan llegando, el lease sigue vivo
            }

            @Override
            public void onError(Throwable t) {
                log.warn("[election] keepAlive del lease se corto ({}): {}", instanceId, t.getMessage());
            }

            @Override
            public void onCompleted() {
                log.warn("[election] keepAlive del lease finalizo ({})", instanceId);
            }
        });

        log.info("[election] {} entrando a la eleccion '{}' (lease {}s)...", instanceId, electionName, leaseTtlSeconds);

        // campaign() no resuelve hasta ganar: si ya hay lider, esta instancia
        // se queda candidata (isLeader() = false) hasta que le toque. No
        // bloqueamos el arranque del servicio esperando a ganar la eleccion.
        electionClient.campaign(election, leaseId, proposal)
                .thenAccept(resp -> {
                    leaderKey.set(resp.getLeader());
                    currentLeaderId.set(instanceId);
                    isLeader.set(true);
                    log.info("[election] {} es ahora LIDER de '{}'", instanceId, electionName);
                })
                .exceptionally(err -> {
                    log.error("[election] {} fallo al competir por el liderazgo: {}", instanceId, err.getMessage());
                    return null;
                });

        observarCambiosDeLider(election);
    }

    /**
     * Observa el valor actual del lider (para loguear/exponer quien es el
     * lider incluso en las replicas que no lo son). Si esta instancia deja
     * de ser el valor observado, se apaga su propio isLeader.
     */
    private void observarCambiosDeLider(ByteSequence election) {
        electionClient.observe(election, new Election.Listener() {
            @Override
            public void onNext(LeaderResponse response) {
                String liderActual = response.getKv().getValue().toString(StandardCharsets.UTF_8);
                currentLeaderId.set(liderActual);
                boolean soyYo = liderActual.equals(instanceId);
                if (isLeader.get() && !soyYo) {
                    log.warn("[election] {} perdio el liderazgo, ahora es {}", instanceId, liderActual);
                }
                isLeader.set(soyYo);
            }

            @Override
            public void onError(Throwable throwable) {
                log.warn("[election] observe() de '{}' fallo: {}", electionName, throwable.getMessage());
            }

            @Override
            public void onCompleted() {
                // la eleccion quedo sin lider (todas las replicas cayeron); nada que hacer
            }
        });
    }

    /** Consultado por los @Scheduled: solo la replica lider debe ejecutar. */
    public boolean isLeader() {
        return isLeader.get();
    }

    public String instanceId() {
        return instanceId;
    }

    public String currentLeaderId() {
        return currentLeaderId.get();
    }

    @PreDestroy
    public void detener() {
        try {
            LeaderKey lk = leaderKey.get();
            if (lk != null && electionClient != null) {
                electionClient.resign(lk).get(); // renuncia limpia, otro puede ganar de inmediato
            }
            if (keepAliveHandle != null) keepAliveHandle.close();
            if (leaseClient != null && leaseId != 0) leaseClient.revoke(leaseId).get();
            if (client != null) client.close();
            log.info("[election] {} salio de la eleccion '{}'", instanceId, electionName);
        } catch (Exception e) {
            log.warn("[election] error cerrando la eleccion de forma limpia: {}", e.getMessage());
        }
    }
}
