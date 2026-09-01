package ec.uteq.sga.soporte.domain;

import ec.uteq.sga.soporte.dto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Pruebas Unitarias: Entidades de Dominio y DTOs de Soporte")
class TicketDomainTest {

    @Test
    @DisplayName("1. Instanciar Ticket -- Verifica getters y setters")
    void ticketGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setNumeroTicket("TK-001");
        ticket.setTitulo("Impresora atascada");
        ticket.setDescripcion("Papel atascado en bandeja 2");
        ticket.setCategoria("HARDWARE");
        ticket.setPrioridad("MEDIO");
        ticket.setEstado("ABIERTO");
        ticket.setCreadoPor("docente1");
        ticket.setAsignadoA("tecnico1");
        ticket.setSolucionAplicada("Limpieza de rodillos");
        ticket.setFechaCreacion(now);
        ticket.setFechaResolucion(now.plusHours(1));

        assertThat(ticket.getId()).isEqualTo(1L);
        assertThat(ticket.getNumeroTicket()).isEqualTo("TK-001");
        assertThat(ticket.getTitulo()).isEqualTo("Impresora atascada");
        assertThat(ticket.getDescripcion()).isEqualTo("Papel atascado en bandeja 2");
        assertThat(ticket.getCategoria()).isEqualTo("HARDWARE");
        assertThat(ticket.getPrioridad()).isEqualTo("MEDIO");
        assertThat(ticket.getEstado()).isEqualTo("ABIERTO");
        assertThat(ticket.getCreadoPor()).isEqualTo("docente1");
        assertThat(ticket.getAsignadoA()).isEqualTo("tecnico1");
        assertThat(ticket.getSolucionAplicada()).isEqualTo("Limpieza de rodillos");
        assertThat(ticket.getFechaCreacion()).isEqualTo(now);
        assertThat(ticket.getFechaResolucion()).isEqualTo(now.plusHours(1));
    }

    @Test
    @DisplayName("2. Instanciar TicketComentario -- Verifica getters y setters")
    void ticketComentarioGettersAndSetters() {
        LocalDateTime now = LocalDateTime.now();
        TicketComentario comentario = new TicketComentario();
        comentario.setId(10L);
        comentario.setIdTicket(1L);
        comentario.setAutor("tecnico1");
        comentario.setContenido("Revisando equipo");
        comentario.setNotaInterna(true);
        comentario.setFechaCreacion(now);

        assertThat(comentario.getId()).isEqualTo(10L);
        assertThat(comentario.getIdTicket()).isEqualTo(1L);
        assertThat(comentario.getAutor()).isEqualTo("tecnico1");
        assertThat(comentario.getContenido()).isEqualTo("Revisando equipo");
        assertThat(comentario.getNotaInterna()).isTrue();
        assertThat(comentario.getFechaCreacion()).isEqualTo(now);
    }

    @Test
    @DisplayName("3. Enumeraciones EstadoTicket y PrioridadTicket -- Valores consistentes")
    void enumValues() {
        assertThat(EstadoTicket.ABIERTO.name()).isEqualTo("ABIERTO");
        assertThat(EstadoTicket.EN_PROCESO.name()).isEqualTo("EN_PROCESO");
        assertThat(EstadoTicket.RESUELTO.name()).isEqualTo("RESUELTO");
        assertThat(EstadoTicket.CERRADO.name()).isEqualTo("CERRADO");

        assertThat(PrioridadTicket.BAJO.name()).isEqualTo("BAJO");
        assertThat(PrioridadTicket.MEDIO.name()).isEqualTo("MEDIO");
        assertThat(PrioridadTicket.ALTO.name()).isEqualTo("ALTO");
        assertThat(PrioridadTicket.CRITICO.name()).isEqualTo("CRITICO");
    }

    @Test
    @DisplayName("4. DTOs de Soporte -- Creación y validación de Records")
    void dtosCoverage() {
        TicketRequest tr = new TicketRequest("Titulo", "Desc", "RED", "ALTO");
        assertThat(tr.titulo()).isEqualTo("Titulo");
        assertThat(tr.descripcion()).isEqualTo("Desc");
        assertThat(tr.categoria()).isEqualTo("RED");
        assertThat(tr.prioridad()).isEqualTo("ALTO");

        ActualizarTicketRequest atr = new ActualizarTicketRequest("RESUELTO", "tecnico1", "Solucion");
        assertThat(atr.estado()).isEqualTo("RESUELTO");
        assertThat(atr.asignadoA()).isEqualTo("tecnico1");
        assertThat(atr.solucionAplicada()).isEqualTo("Solucion");

        EscalarTicketRequest etr = new EscalarTicketRequest("CRITICO", "director1", "Motivo");
        assertThat(etr.nuevaPrioridad()).isEqualTo("CRITICO");
        assertThat(etr.nuevoAsignado()).isEqualTo("director1");
        assertThat(etr.motivo()).isEqualTo("Motivo");

        ComentarioRequest cr = new ComentarioRequest("Contenido", true);
        assertThat(cr.contenido()).isEqualTo("Contenido");
        assertThat(cr.notaInterna()).isTrue();

        CrearTicketDTO ctd = new CrearTicketDTO();
        ctd.setTitulo("T");
        ctd.setDescripcion("D");
        ctd.setCategoriaId(1L);
        ctd.setPrioridad("ALTA");
        assertThat(ctd.getTitulo()).isEqualTo("T");
        assertThat(ctd.getDescripcion()).isEqualTo("D");
        assertThat(ctd.getCategoriaId()).isEqualTo(1L);
        assertThat(ctd.getPrioridad()).isEqualTo("ALTA");

        AgregarComentarioDTO acd = new AgregarComentarioDTO();
        acd.setComentario("Com");
        acd.setEsRespuestaInterna(true);
        assertThat(acd.getComentario()).isEqualTo("Com");
        assertThat(acd.getEsRespuestaInterna()).isTrue();
    }
}
