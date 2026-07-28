package ec.uteq.sga.soporte.repository;

import ec.uteq.sga.soporte.model.Ticket;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends CrudRepository<Ticket, Long> {
    List<Ticket> findByUsuarioSolicitanteId(Long usuarioSolicitanteId);
    List<Ticket> findByTecnicoAsignadoId(Long tecnicoAsignadoId);
    List<Ticket> findByEstado(String estado);
}