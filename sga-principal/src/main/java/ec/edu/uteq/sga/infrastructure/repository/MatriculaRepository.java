package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Matricula;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MatriculaRepository extends JpaRepository<Matricula, Long> {
    List<Matricula> findByAnoLectivo_IdAnoLectivo(Long idAnoLectivo);
    List<Matricula> findByEstudiante_IdEstudiante(Long idEstudiante);
    @Query("SELECT m FROM Matricula m JOIN FETCH m.estudiante e JOIN FETCH m.grado JOIN FETCH m.paralelo WHERE e.representante.idRepresentante = :idRepresentante AND m.estado = 'ACTIVA' ORDER BY e.apellidos, e.nombres")
    List<Matricula> findActivasByRepresentante(@Param("idRepresentante") Long idRepresentante);
    @Query("SELECT m FROM Matricula m JOIN FETCH m.estudiante e JOIN FETCH m.grado JOIN FETCH m.paralelo JOIN FETCH m.anoLectivo WHERE e.representante.idRepresentante = :idRepresentante AND e.idEstudiante = :idEstudiante AND m.estado = 'ACTIVA'")
    List<Matricula> findActivasByRepresentanteAndEstudiante(@Param("idRepresentante") Long idRepresentante, @Param("idEstudiante") Long idEstudiante);
    boolean existsByEstudiante_IdEstudianteAndAnoLectivo_IdAnoLectivo(Long idEstudiante, Long idAnoLectivo);
    List<Matricula> findByGrado_IdGradoAndAnoLectivo_IdAnoLectivo(Long idGrado, Long idAnoLectivo);
    List<Matricula> findByGrado_IdGradoAndParalelo_IdParaleloAndAnoLectivo_IdAnoLectivo(Long idGrado, Long idParalelo, Long idAnoLectivo);
    Optional<Matricula> findTopByAnoLectivo_IdAnoLectivoOrderByNumeroOrdenDesc(Long idAnoLectivo);

    @Query("SELECT COUNT(m) FROM Matricula m WHERE m.grado.idGrado = :idGrado AND m.paralelo.idParalelo = :idParalelo AND m.anoLectivo.idAnoLectivo = :idAnoLectivo AND m.estado = 'ACTIVA'")
    long countByGradoParaleloAnoLectivo(@Param("idGrado") Long idGrado, @Param("idParalelo") Long idParalelo, @Param("idAnoLectivo") Long idAnoLectivo);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.estudiante e LEFT JOIN FETCH e.representante WHERE m.grado.idGrado = :idGrado AND m.anoLectivo.idAnoLectivo = :idAnoLectivo")
    List<Matricula> findByGradoAndAnoLectivoWithEstudiante(@Param("idGrado") Long idGrado, @Param("idAnoLectivo") Long idAnoLectivo);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.estudiante e LEFT JOIN FETCH e.representante WHERE m.grado.idGrado = :idGrado AND m.paralelo.idParalelo = :idParalelo AND m.anoLectivo.idAnoLectivo = :idAnoLectivo")
    List<Matricula> findByGradoParaleloAndAnoLectivoWithEstudiante(@Param("idGrado") Long idGrado, @Param("idParalelo") Long idParalelo, @Param("idAnoLectivo") Long idAnoLectivo);

    @Query("SELECT m FROM Matricula m JOIN FETCH m.estudiante e WHERE m.anoLectivo.idAnoLectivo = :idAnoLectivo ORDER BY e.apellidos, e.nombres")
    List<Matricula> findByAnoLectivoWithEstudiante(@Param("idAnoLectivo") Long idAnoLectivo);
}
