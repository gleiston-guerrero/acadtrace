package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.FichaEstudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FichaEstudianteRepository extends JpaRepository<FichaEstudiante, Long> {
    Optional<FichaEstudiante> findByEstudianteIdEstudiante(Long idEstudiante);
    List<FichaEstudiante> findByEstudiante_IdEstudianteIn(List<Long> ids);
}
