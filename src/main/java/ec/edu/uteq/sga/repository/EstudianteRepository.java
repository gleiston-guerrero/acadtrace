package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.Estudiante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {
    Optional<Estudiante> findByCedula(String cedula);
    boolean existsByCedula(String cedula);
    List<Estudiante> findByEstado(String estado);
    List<Estudiante> findByNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(String nombres, String apellidos);

    @Query("SELECT e FROM Estudiante e LEFT JOIN FETCH e.representante")
    List<Estudiante> findAllWithRepresentante();

    @Query("SELECT e FROM Estudiante e LEFT JOIN FETCH e.representante WHERE LOWER(e.nombres) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(e.apellidos) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Estudiante> searchWithRepresentante(@Param("q") String query);
}