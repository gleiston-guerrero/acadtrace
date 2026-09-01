package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.AnoLectivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AnoLectivoRepository extends JpaRepository<AnoLectivo, Long> {
    boolean existsByNombre(String nombre);
    Optional<AnoLectivo> findByEsActualTrue();
}