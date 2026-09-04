package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Asignacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AsignacionRepository extends JpaRepository<Asignacion, Long> {
    List<Asignacion> findByAnoLectivo_IdAnoLectivo(Long idAnoLectivo);
    List<Asignacion> findByDocente_IdPersona(Long idDocente);
    List<Asignacion> findByGrado_IdGradoAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivoAndActivoTrue(
            Long idGrado, Long idParalelo, Long idAnoLectivo);
    boolean existsByDocente_IdPersonaAndAsignatura_IdAsignaturaAndGrado_IdGradoAndAnoLectivo_IdAnoLectivo(
            Long idDocente, Long idAsignatura, Long idGrado, Long idAnoLectivo);

    boolean existsByAsignatura_IdAsignaturaAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivo(
            Long idAsignatura, Long idParalelo, Long idAnoLectivo);
}
