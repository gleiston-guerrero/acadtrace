package ec.edu.uteq.sga.soporte.repository;

import ec.edu.uteq.sga.soporte.entity.TicketSoporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketSoporte, Long> {

    Optional<TicketSoporte> findByNumeroTicket(String numeroTicket);

    List<TicketSoporte> findByCreadoPorOrderByFechaCreacionDesc(String creadoPor);

    List<TicketSoporte> findByAsignadoAOrderByFechaCreacionDesc(String asignadoA);

    List<TicketSoporte> findByEstadoOrderByFechaCreacionDesc(String estado);

    List<TicketSoporte> findAllByOrderByFechaCreacionDesc();

    boolean existsByNumeroTicket(String numeroTicket);

    @Query("SELECT COUNT(t) FROM TicketSoporte t WHERE t.estado = :estado")
    long contarPorEstado(String estado);

    @Query("SELECT MAX(t.idTicket) FROM TicketSoporte t")
    Long maxId();

    List<TicketSoporte> findByTituloContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
            String titulo, String descripcion);
}