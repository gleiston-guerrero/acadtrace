package ec.uteq.sga.soporte.repository;

import ec.uteq.sga.soporte.model.TicketComentario;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketComentarioRepository extends CrudRepository<TicketComentario, Long> {
    List<TicketComentario> findByIdTicketOrderByFechaCreacionAsc(Long idTicket);
}