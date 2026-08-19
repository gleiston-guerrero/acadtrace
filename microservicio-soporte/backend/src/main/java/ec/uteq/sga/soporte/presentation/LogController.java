package ec.uteq.sga.soporte.presentation;

import ec.uteq.sga.soporte.infrastructure.logging.MemoryLogAppender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Expone los últimos errores/warnings de ESTE proceso (sga-soporte) para el
 * panel "Logs de fallos" del Dashboard, evitando tener que revisar logs por
 * SSH/docker logs. Protegido por JwtAuthFilter (SOPORTE_TECNICO/DIRECTOR) al
 * vivir bajo /api/soporte/*.
 *
 * NOTA: hoy solo cubre sga-soporte. Para que el Dashboard muestre también
 * los fallos de sga-principal, sga-docente y sga-secretaria, cada uno
 * necesita un endpoint equivalente (mismo formato de respuesta); el
 * frontend ya está preparado para agregarlos apenas existan (ver
 * FUENTES_LOGS en Dashboard.jsx).
 */
@RestController
public class LogController {

    @GetMapping("/api/soporte/logs")
    public List<Map<String, Object>> logsRecientes(@RequestParam(defaultValue = "50") int limite) {
        int limiteSeguro = Math.min(Math.max(limite, 1), 200);
        return MemoryLogAppender.ultimos(limiteSeguro);
    }
}
