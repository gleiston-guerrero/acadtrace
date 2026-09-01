package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.EscalaCalificaciones;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface EscalaCalificacionesRepository extends JpaRepository<EscalaCalificaciones, Long> {
    List<EscalaCalificaciones> findByAnoLectivo_IdAnoLectivo(Long idAnoLectivo);
    Optional<EscalaCalificaciones> findByAnoLectivo_IdAnoLectivoAndNotaMinimaLessThanEqualAndNotaMaximaGreaterThanEqual(
            Long idAnoLectivo, BigDecimal nota1, BigDecimal nota2);
}