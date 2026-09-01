package ec.uteq.sga.soporte.presentation;

import ec.uteq.sga.soporte.application.TicketService;
import ec.uteq.sga.soporte.dto.ActualizarTicketRequest;
import ec.uteq.sga.soporte.dto.ComentarioRequest;
import ec.uteq.sga.soporte.dto.EscalarTicketRequest;
import ec.uteq.sga.soporte.dto.TicketRequest;
import ec.uteq.sga.soporte.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: TicketController")
class TicketControllerTest {

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private AuthenticatedUser userTecnico;
    private Map<String, Object> sampleTicket;

    @BeforeEach
    void setUp() {
        userTecnico = new AuthenticatedUser("tecnico1", List.of("SOPORTE_TECNICO"));
        sampleTicket = Map.of("idTicket", 1L, "titulo", "Falla de red", "creadoPor", "docente1");
    }

    @Test
    @DisplayName("1. Listar y Mis Tickets")
    void listarYMisTickets() {
        given(ticketService.listar(userTecnico)).willReturn(List.of(sampleTicket));
        given(ticketService.misTickets("tecnico1")).willReturn(List.of(sampleTicket));

        List<Map<String, Object>> todos = ticketController.listar(userTecnico);
        assertThat(todos).hasSize(1);

        List<Map<String, Object>> mis = ticketController.misTickets(userTecnico);
        assertThat(mis).hasSize(1);
    }

    @Test
    @DisplayName("2. Estadísticas y Reportes")
    void estadisticasYReportes() {
        given(ticketService.estadisticas(userTecnico)).willReturn(Map.of("total", 5L));
        given(ticketService.reportes(userTecnico)).willReturn(Map.of("general", Map.of()));

        Map<String, Object> stats = ticketController.estadisticas(userTecnico);
        assertThat(stats).containsEntry("total", 5L);

        Map<String, Object> reports = ticketController.reportes(userTecnico);
        assertThat(reports).containsKey("general");
    }

    @Test
    @DisplayName("3. Obtener por ID y Crear Ticket")
    void obtenerYCrear() {
        given(ticketService.obtener(1L, userTecnico)).willReturn(sampleTicket);

        TicketRequest req = new TicketRequest("Titulo", "Desc", "RED", "ALTO");
        given(ticketService.crear(req, "tecnico1")).willReturn(sampleTicket);

        Map<String, Object> obtenido = ticketController.obtener(1L, userTecnico);
        assertThat(obtenido).isNotNull();

        ResponseEntity<Map<String, Object>> resp = ticketController.crear(req, userTecnico);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resp.getBody()).isEqualTo(sampleTicket);
    }

    @Test
    @DisplayName("4. Actualizar y Escalar Ticket")
    void actualizarYEscalar() {
        ActualizarTicketRequest actReq = new ActualizarTicketRequest("EN_PROCESO", "tecnico1", null);
        given(ticketService.actualizar(1L, actReq, userTecnico)).willReturn(sampleTicket);

        EscalarTicketRequest escReq = new EscalarTicketRequest("CRITICO", "director1", "Urgente");
        given(ticketService.escalar(1L, escReq, userTecnico)).willReturn(sampleTicket);

        Map<String, Object> actResult = ticketController.actualizar(1L, actReq, userTecnico);
        assertThat(actResult).isEqualTo(sampleTicket);

        Map<String, Object> escResult = ticketController.escalar(1L, escReq, userTecnico);
        assertThat(escResult).isEqualTo(sampleTicket);
    }

    @Test
    @DisplayName("5. Historial, Listar Comentarios y Comentar")
    void historialYComentarios() {
        given(ticketService.listarHistorial(1L, userTecnico)).willReturn(List.of(Map.of("campo", "ESTADO")));
        given(ticketService.listarComentarios(1L)).willReturn(List.of(Map.of("contenido", "Nota")));

        ComentarioRequest comReq = new ComentarioRequest("Nota nueva", false);
        given(ticketService.comentar(1L, comReq, userTecnico)).willReturn(Map.of("idComentario", 1L));

        List<Map<String, Object>> hist = ticketController.historial(1L, userTecnico);
        assertThat(hist).hasSize(1);

        List<Map<String, Object>> coms = ticketController.listarComentarios(1L);
        assertThat(coms).hasSize(1);

        ResponseEntity<Map<String, Object>> respCom = ticketController.comentar(1L, comReq, userTecnico);
        assertThat(respCom.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respCom.getBody()).containsEntry("idComentario", 1L);
    }
}
