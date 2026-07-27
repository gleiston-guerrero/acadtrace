package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.MallaCurricular;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MallaCurricularRepository extends JpaRepository<MallaCurricular, Long> {

    @Query("SELECT m FROM MallaCurricular m JOIN FETCH m.asignatura WHERE m.grado.idGrado = :idGrado AND m.anoLectivo.idAnoLectivo = :idAnoLectivo ORDER BY m.horasSemana DESC, m.asignatura.nombre")
    List<MallaCurricular> findByGradoAnoWithAsignatura(@Param("idGrado") Long idGrado, @Param("idAnoLectivo") Long idAnoLectivo);

    boolean existsByGrado_IdGradoAndAsignatura_IdAsignaturaAndAnoLectivo_IdAnoLectivo(Long idGrado, Long idAsignatura, Long idAnoLectivo);
}
