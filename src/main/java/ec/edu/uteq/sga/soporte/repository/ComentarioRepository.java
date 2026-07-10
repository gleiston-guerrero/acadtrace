package ec.edu.uteq.sga.soporte.repository;

import ec.edu.uteq.sga.soporte.entity.ComentarioTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioRepository extends JpaRepository<ComentarioTicket, Long> {

    List<ComentarioTicket> findByTicket_IdTicketOrderByFechaCreacionAsc(Long idTicket);

    List<ComentarioTicket> findByTicket_IdTicketAndNotaInternaFalseOrderByFechaCreacionAsc(Long idTicket);
}