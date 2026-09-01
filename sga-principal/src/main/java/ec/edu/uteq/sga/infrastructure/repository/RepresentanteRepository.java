package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Representante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface RepresentanteRepository extends JpaRepository<Representante, Long> {
    Optional<Representante> findByCedula(String cedula);
    boolean existsByCedula(String cedula);
}