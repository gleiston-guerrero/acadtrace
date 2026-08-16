package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.PeriodoHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PeriodoHorarioRepository extends JpaRepository<PeriodoHorario, Integer> {
    List<PeriodoHorario> findByActivoTrueOrderByOrdenAsc();
}
