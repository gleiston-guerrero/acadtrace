package ec.uteq.sga.soporte.application;

import ec.uteq.sga.soporte.common.ApiException;
import ec.uteq.sga.soporte.domain.port.TicketRepositoryPort;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias: TicketService (Seguridad RBAC, Control IDOR y Operaciones)")
class TicketServiceTest {

    @Mock
    private TicketRepositoryPort tickets;

    @InjectMocks
    private TicketService ticketService;

    private AuthenticatedUser userTecnico;
    private AuthenticatedUser userDirector;
    private AuthenticatedUser userDocente;
    private Map<String, Object> sampleTicket;

    @BeforeEach
    void setUp() {
        userTecnico = new AuthenticatedUser("tecnico1", List.of("SOPORTE_TECNICO"));
        userDirector = new AuthenticatedUser("director1", List.of("DIRECTOR"));
        userDocente = new AuthenticatedUser("docente1", List.of("DOCENTE"));

        sampleTicket = Map.of(
                "idTicket", 100L,
                "numero", "TK-100",
                "titulo", "Falla en proyector",
                "descripcion", "No enciende el equipo",
                "categoria", "HARDWARE",
                "prioridad", "ALTA",
                "estado", "ABIERTO",
                "creadoPor", "docente1",
                "asignadoA", "tecnico1"
        );
    }

    @Test
    @DisplayName("1. Listar tickets -- Usuario con rol SOPORTE_TECNICO -- Retorna lista completa")
    void listar_whenTecnico_returnsTickets() {
        given(tickets.listarTodos()).willReturn(List.of(sampleTicket));

        List<Map<String, Object>> result = ticketService.listar(userTecnico);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("numero")).isEqualTo("TK-100");
        verify(tickets, times(1)).listarTodos();
    }

    @Test
    @DisplayName("2. Listar tickets -- Usuario con rol DOCENTE -- Lanza 403 Forbidden")
    void listar_whenDocente_throwsForbidden() {
        assertThatThrownBy(() -> ticketService.listar(userDocente))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Accion exclusiva del equipo de soporte");

        verify(tickets, never()).listarTodos();
    }

    @Test
    @DisplayName("3. Mis tickets -- Consulta por autor -- Retorna tickets creados por el usuario")
    void misTickets_returnsUserTickets() {
        given(tickets.listarPorCreador("docente1")).willReturn(List.of(sampleTicket));

        List<Map<String, Object>> result = ticketService.misTickets("docente1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("creadoPor")).isEqualTo("docente1");
        verify(tickets).listarPorCreador("docente1");
    }

    @Test
    @DisplayName("4. Estadísticas y Reportes -- Técnico autorizado -- Retorna métricas")
    void estadisticasYReportes_whenTecnico_returnsData() {
        given(tickets.estadisticas()).willReturn(Map.of("total", 10L, "abiertos", 2L));
        given(tickets.reportePorCategoria()).willReturn(List.of(Map.of("categoria", "HARDWARE", "total", 5L)));
        given(tickets.reportePorTecnico()).willReturn(List.of(Map.of("tecnico", "tecnico1", "resueltos", 4L)));
        given(tickets.reporteGeneral()).willReturn(Map.of("promedioHoras", 2.5));

        Map<String, Object> stats = ticketService.estadisticas(userTecnico);
        assertThat(stats).containsEntry("total", 10L);

        Map<String, Object> reports = ticketService.reportes(userDirector);
        assertThat(reports).containsKey("porCategoria");
        assertThat(reports).containsKey("porTecnico");
        assertThat(reports).containsKey("general");
    }

    @Test
    @DisplayName("5. Estadísticas -- Docente no autorizado -- Lanza 403 Forbidden")
    void estadisticas_whenDocente_throwsForbidden() {
        assertThatThrownBy(() -> ticketService.estadisticas(userDocente))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Accion exclusiva del equipo de soporte");
    }

    @Test
    @DisplayName("6. Obtener ticket por ID -- Propietario del ticket -- Retorna detalle")
    void obtener_whenOwner_returnsTicket() {
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        Map<String, Object> result = ticketService.obtener(100L, userDocente);

        assertThat(result).isNotNull();
        assertThat(result.get("idTicket")).isEqualTo(100L);
    }

    @Test
    @DisplayName("7. Obtener ticket por ID -- Usuario ajeno sin rol técnico (Control IDOR) -- Lanza 403 Forbidden")
    void obtener_whenNotOwnerAndNotTecnico_throwsForbiddenIDOR() {
        AuthenticatedUser otroDocente = new AuthenticatedUser("docente2", List.of("DOCENTE"));
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        assertThatThrownBy(() -> ticketService.obtener(100L, otroDocente))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No tiene acceso a este ticket");
    }

    @Test
    @DisplayName("8. Obtener ticket por ID -- Técnico sobre ticket de cualquier usuario -- Retorna detalle")
    void obtener_whenTecnicoAndDifferentOwner_returnsTicket() {
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        Map<String, Object> result = ticketService.obtener(100L, userTecnico);

        assertThat(result).isNotNull();
        assertThat(result.get("idTicket")).isEqualTo(100L);
    }

    @Test
    @DisplayName("9. Obtener ticket por ID -- ID Inexistente -- Lanza 404 Not Found")
    void obtener_whenNotExists_throwsNotFound() {
        given(tickets.buscarPorId(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.obtener(999L, userTecnico))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ticket no encontrado");
    }

    @Test
    @DisplayName("10. Crear ticket -- Datos válidos -- Retorna ticket creado")
    void crear_whenValid_returnsTicket() {
        TicketRequest req = new TicketRequest("Falla de red", "Sin conexión en aula 3", "RED", "MEDIO");
        given(tickets.crear(anyString(), eq("Falla de red"), eq("Sin conexión en aula 3"), eq("RED"), eq("MEDIO"), eq("docente1")))
                .willReturn(101L);
        given(tickets.buscarPorId(101L)).willReturn(Optional.of(Map.of("idTicket", 101L, "titulo", "Falla de red")));

        Map<String, Object> result = ticketService.crear(req, "docente1");

        assertThat(result).isNotNull();
        assertThat(result.get("idTicket")).isEqualTo(101L);
        verify(tickets).crear(anyString(), anyString(), anyString(), anyString(), anyString(), eq("docente1"));
    }

    @Test
    @DisplayName("11. Crear ticket -- Prioridad o Categoría inválida -- Lanza 400 Bad Request")
    void crear_whenInvalidFields_throwsBadRequest() {
        TicketRequest reqPrio = new TicketRequest("Falla", "Desc", "RED", "SUPER_URGENTE");
        assertThatThrownBy(() -> ticketService.crear(reqPrio, "docente1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Prioridad inválida");

        TicketRequest reqCat = new TicketRequest("Falla", "Desc", "CATEGORIA_INVENTADA", "ALTO");
        assertThatThrownBy(() -> ticketService.crear(reqCat, "docente1"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Categoría inválida");
    }

    @Test
    @DisplayName("12. Crear ticket desde Incidencia gRPC -- Tolera valores no canónicos")
    void crearDesdeIncidencia_handlesDefaults() {
        given(tickets.crear(anyString(), eq("Falla DB"), eq("Conexión perdida"), eq("OTRO"), eq("ALTO"), eq("sistema:sga-docente")))
                .willReturn(102L);
        given(tickets.buscarPorId(102L)).willReturn(Optional.of(Map.of("idTicket", 102L, "titulo", "Falla DB")));

        Map<String, Object> result = ticketService.crearDesdeIncidencia("sga-docente", "Falla DB", "Conexión perdida", "INVALID_CAT", "INVALID_PRIO");

        assertThat(result).isNotNull();
        assertThat(result.get("idTicket")).isEqualTo(102L);
        verify(tickets).crear(anyString(), eq("Falla DB"), eq("Conexión perdida"), eq("OTRO"), eq("ALTO"), eq("sistema:sga-docente"));
    }

    @Test
    @DisplayName("13. Actualizar ticket -- Técnico asignado -- Actualiza estado y registra historial")
    void actualizar_whenTecnico_updatesTicket() {
        ActualizarTicketRequest req = new ActualizarTicketRequest("EN_PROCESO", "tecnico2", "Revisando switch");
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        Map<String, Object> result = ticketService.actualizar(100L, req, userTecnico);

        assertThat(result).isNotNull();
        verify(tickets).actualizarEstado(100L, "EN_PROCESO", "tecnico2", "Revisando switch");
        verify(tickets).registrarHistorial(100L, "ESTADO", "ABIERTO", "EN_PROCESO", "tecnico1");
        verify(tickets).registrarHistorial(100L, "ASIGNADO_A", "tecnico1", "tecnico2", "tecnico1");
    }

    @Test
    @DisplayName("14. Actualizar ticket -- Estado inválido -- Lanza 400 Bad Request")
    void actualizar_whenInvalidState_throwsBadRequest() {
        ActualizarTicketRequest req = new ActualizarTicketRequest("ESTADO_INEXISTENTE", "tecnico1", null);

        assertThatThrownBy(() -> ticketService.actualizar(100L, req, userTecnico))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Estado inválido");
    }

    @Test
    @DisplayName("15. Escalar ticket -- Técnico con justificación -- Escala y registra nota interna")
    void escalar_whenTecnico_escalatesTicket() {
        EscalarTicketRequest req = new EscalarTicketRequest("CRITICO", "director1", "Requiere aprobación de presupuesto");
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        Map<String, Object> result = ticketService.escalar(100L, req, userTecnico);

        assertThat(result).isNotNull();
        verify(tickets).actualizarPrioridadYAsignado(100L, "CRITICO", "director1");
        verify(tickets).registrarHistorial(100L, "PRIORIDAD", "ALTA", "CRITICO", "tecnico1");
        verify(tickets).registrarHistorial(100L, "ASIGNADO_A", "tecnico1", "director1", "tecnico1");
        verify(tickets).agregarComentario(100L, "tecnico1", "Escalamiento: Requiere aprobación de presupuesto", true);
    }

    @Test
    @DisplayName("16. Escalar ticket -- Ticket ya cerrado -- Lanza 400 Bad Request")
    void escalar_whenClosedTicket_throwsBadRequest() {
        Map<String, Object> ticketCerrado = Map.of(
                "idTicket", 100L,
                "estado", "CERRADO",
                "prioridad", "ALTA",
                "creadoPor", "docente1"
        );
        EscalarTicketRequest req = new EscalarTicketRequest("CRITICO", "director1", "Motivo");
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(ticketCerrado));

        assertThatThrownBy(() -> ticketService.escalar(100L, req, userTecnico))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No se puede escalar un ticket cerrado");
    }

    @Test
    @DisplayName("17. Escalar ticket -- Sin cambios de prioridad ni asignado -- Lanza 400 Bad Request")
    void escalar_whenNoChanges_throwsBadRequest() {
        EscalarTicketRequest req = new EscalarTicketRequest(null, null, "Motivo sin cambios");
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));

        assertThatThrownBy(() -> ticketService.escalar(100L, req, userTecnico))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("El escalamiento debe cambiar la prioridad y/o reasignar el ticket");
    }

    @Test
    @DisplayName("18. Historial y Comentarios -- Consulta autorizada")
    void historialYComentarios_returnsData() {
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));
        given(tickets.listarHistorial(100L)).willReturn(List.of(Map.of("campo", "ESTADO")));
        given(tickets.listarComentarios(100L)).willReturn(List.of(Map.of("contenido", "En revisión")));

        List<Map<String, Object>> historial = ticketService.listarHistorial(100L, userTecnico);
        assertThat(historial).hasSize(1);

        List<Map<String, Object>> comentarios = ticketService.listarComentarios(100L);
        assertThat(comentarios).hasSize(1);
    }

    @Test
    @DisplayName("19. Comentar ticket -- Usuario regular y nota interna por técnico")
    void comentar_registersCommentAndInternalNotes() {
        given(tickets.buscarPorId(100L)).willReturn(Optional.of(sampleTicket));
        given(tickets.agregarComentario(100L, "docente1", "Mensaje docente", false))
                .willReturn(Map.of("idComentario", 1L, "contenido", "Mensaje docente"));
        given(tickets.agregarComentario(100L, "tecnico1", "Nota interna", true))
                .willReturn(Map.of("idComentario", 2L, "contenido", "Nota interna", "notaInterna", true));

        ComentarioRequest reqDocente = new ComentarioRequest("Mensaje docente", false);
        Map<String, Object> resDocente = ticketService.comentar(100L, reqDocente, userDocente);
        assertThat(resDocente).containsEntry("contenido", "Mensaje docente");

        ComentarioRequest reqTecnico = new ComentarioRequest("Nota interna", true);
        Map<String, Object> resTecnico = ticketService.comentar(100L, reqTecnico, userTecnico);
        assertThat(resTecnico).containsEntry("notaInterna", true);
    }
}
