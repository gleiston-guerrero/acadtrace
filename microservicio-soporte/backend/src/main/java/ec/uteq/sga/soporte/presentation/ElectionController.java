package ec.uteq.sga.soporte.presentation;

import ec.uteq.sga.soporte.election.LeaderElectionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Estado de la eleccion de lider de esta instancia. Util para la demo, en
 * complemento a `docker exec etcd etcdctl get --prefix /sga/leader`: ese
 * comando muestra el estado global en etcd, este endpoint muestra que ve
 * cada replica desde adentro.
 */
@RestController
public class ElectionController {

    private final LeaderElectionService leaderElection;

    public ElectionController(LeaderElectionService leaderElection) {
        this.leaderElection = leaderElection;
    }

    @GetMapping("/api/soporte/election/status")
    public Map<String, Object> status() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("instanceId", leaderElection.instanceId());
        body.put("isLeader", leaderElection.isLeader());
        body.put("currentLeaderId", leaderElection.currentLeaderId());
        return body;
    }
}
