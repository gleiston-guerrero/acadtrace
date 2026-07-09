package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.Paralelo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ParaleloRepository extends JpaRepository<Paralelo, Long> {
    List<Paralelo> findByGradoIdGradoOrderByLetra(Long idGrado);
    List<Paralelo> findByGradoIdGradoAndActivoTrueOrderByLetra(Long idGrado);
    boolean existsByGradoIdGradoAndLetra(Long idGrado, String letra);
}
