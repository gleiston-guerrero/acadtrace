package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    Page<Notificacion> findByUsuario_IdUsuarioOrderByFechaDesc(Long idUsuario, Pageable pageable);

    long countByUsuario_IdUsuarioAndLeidaFalse(Long idUsuario);

    @Modifying
    @Query("UPDATE Notificacion n SET n.leida = true WHERE n.usuario.idUsuario = :idUsuario AND n.leida = false")
    int marcarTodasLeidas(@Param("idUsuario") Long idUsuario);
}
