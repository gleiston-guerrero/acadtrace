package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.TipoAporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TipoAporteRepository extends JpaRepository<TipoAporte, Long> {
    List<TipoAporte> findByAnoLectivo_IdAnoLectivoOrderByTipoEvaluacionAscOrdenAsc(Long idAnoLectivo);
    boolean existsByAnoLectivo_IdAnoLectivoAndNombreIgnoreCase(Long idAnoLectivo, String nombre);
}
