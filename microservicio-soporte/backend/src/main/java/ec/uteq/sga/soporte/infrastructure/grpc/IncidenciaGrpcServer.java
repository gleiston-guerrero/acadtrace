package ec.uteq.sga.soporte.infrastructure.grpc;

import ec.uteq.sga.soporte.application.TicketService;
import ec.uteq.sga.soporte.grpc.incidencias.IncidenciaServiceGrpc;
import ec.uteq.sga.soporte.grpc.incidencias.ReportarIncidenciaRequest;
import ec.uteq.sga.soporte.grpc.incidencias.ReportarIncidenciaResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Lado servidor del contrato definido en incidencias.proto. Permite que
 * sga-principal, sga-docente y sga-secretaria reporten automaticamente una
 * falla propia como ticket de soporte, sin pasar por el REST protegido con
 * JWT de usuario (ese sigue siendo solo para personas que crean tickets
 * manualmente desde el portal).
 *
 * Protegido por InternalAuthInterceptor (internal_token), igual que el resto
 * de la malla gRPC interna del proyecto.
 */
@GrpcService
public class IncidenciaGrpcServer extends IncidenciaServiceGrpc.IncidenciaServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(IncidenciaGrpcServer.class);

    private final TicketService ticketService;

    public IncidenciaGrpcServer(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public void reportarIncidencia(ReportarIncidenciaRequest request, StreamObserver<ReportarIncidenciaResponse> responseObserver) {
        log.info("[grpc] Incidencia reportada por {}: {}", request.getServicioOrigen(), request.getTitulo());

        Map<String, Object> ticket = ticketService.crearDesdeIncidencia(
                request.getServicioOrigen(),
                request.getTitulo(),
                request.getDescripcion(),
                request.getCategoria(),
                request.getPrioridad()
        );

        ReportarIncidenciaResponse response = ReportarIncidenciaResponse.newBuilder()
                .setIdTicket(((Number) ticket.get("id")).longValue())
                .setNumeroTicket(String.valueOf(ticket.get("numeroTicket")))
                .setEstado(String.valueOf(ticket.get("estado")))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
