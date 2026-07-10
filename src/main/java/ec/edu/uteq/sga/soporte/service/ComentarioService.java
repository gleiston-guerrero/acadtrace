package ec.edu.uteq.sga.soporte.service;

import ec.edu.uteq.sga.soporte.dto.comentario.*;
import ec.edu.uteq.sga.soporte.entity.ComentarioTicket;
import ec.edu.uteq.sga.soporte.entity.TicketSoporte;
import ec.edu.uteq.sga.soporte.repository.ComentarioRepository;
import ec.edu.uteq.sga.soporte.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepo;
    private final TicketRepository ticketRepo;

    @Transactional(readOnly = true)
    public List<ComentarioResponseDTO> listarPorTicket(Long idTicket, boolean soloPublicos) {
        List<ComentarioTicket> lista = soloPublicos
                ? comentarioRepo.findByTicket_IdTicketAndNotaInternaFalseOrderByFechaCreacionAsc(idTicket)
                : comentarioRepo.findByTicket_IdTicketOrderByFechaCreacionAsc(idTicket);
        return lista.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ComentarioResponseDTO agregar(Long idTicket, ComentarioRequestDTO dto, String autor) {
        TicketSoporte ticket = ticketRepo.findById(idTicket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket no encontrado"));

        if ("CERRADO".equals(ticket.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede comentar un ticket cerrado");
        }

        ComentarioTicket comentario = ComentarioTicket.builder()
                .ticket(ticket)
                .contenido(dto.getContenido())
                .autor(autor)
                .notaInterna(dto.isNotaInterna())
                .build();

        return toDTO(comentarioRepo.save(comentario));
    }

    private ComentarioResponseDTO toDTO(ComentarioTicket c) {
        return ComentarioResponseDTO.builder()
                .idComentario(c.getIdComentario())
                .idTicket(c.getTicket().getIdTicket())
                .contenido(c.getContenido())
                .autor(c.getAutor())
                .notaInterna(c.isNotaInterna())
                .fechaCreacion(c.getFechaCreacion())
                .build();
    }
}