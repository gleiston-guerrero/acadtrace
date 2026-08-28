package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.NivelEducativo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface NivelEducativoRepository extends JpaRepository<NivelEducativo, Long> {
    Optional<NivelEducativo> findByNombre(String nombre);
    List<NivelEducativo> findByTipoEscala(String tipoEscala);
}