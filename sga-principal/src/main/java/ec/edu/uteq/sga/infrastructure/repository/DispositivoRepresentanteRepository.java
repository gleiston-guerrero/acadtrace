package ec.edu.uteq.sga.infrastructure.repository;
import ec.edu.uteq.sga.domain.entity.DispositivoRepresentante;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface DispositivoRepresentanteRepository extends JpaRepository<DispositivoRepresentante,Long> {
    Optional<DispositivoRepresentante> findByToken(String token);
    List<DispositivoRepresentante> findByUsuario_IdUsuarioAndActivoTrue(Long idUsuario);
}
