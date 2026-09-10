package ec.edu.uteq.sga.infrastructure.repository;
import ec.edu.uteq.sga.domain.entity.EventoNotificacionPush;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EventoNotificacionPushRepository extends JpaRepository<EventoNotificacionPush,Long> {
    Optional<EventoNotificacionPush> findByClaveEventoAndUsuario_IdUsuario(String clave, Long idUsuario);
}
