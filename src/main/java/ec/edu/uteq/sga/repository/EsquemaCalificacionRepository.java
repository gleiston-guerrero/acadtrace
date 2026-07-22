package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.EsquemaCalificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EsquemaCalificacionRepository extends JpaRepository<EsquemaCalificacion, Long> {
    Optional<EsquemaCalificacion> findByAnoLectivo_IdAnoLectivo(Long idAnoLectivo);
}
