package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.PeriodoEvaluacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PeriodoEvaluacionRepository extends JpaRepository<PeriodoEvaluacion, Long> {
    List<PeriodoEvaluacion> findByAnoLectivo_IdAnoLectivo(Long idAnoLectivo);
    Optional<PeriodoEvaluacion> findByAnoLectivo_IdAnoLectivoAndActivoTrue(Long idAnoLectivo);
}