package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.EstadoCadenaAuditoria;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EstadoCadenaAuditoriaRepository
        extends JpaRepository<EstadoCadenaAuditoria, Short> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e
            FROM EstadoCadenaAuditoria e
            WHERE e.idEstado = :idEstado
            """)
    Optional<EstadoCadenaAuditoria> buscarParaActualizar(
            @Param("idEstado") Short idEstado
    );
}
