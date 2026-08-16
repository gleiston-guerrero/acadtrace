package ec.edu.uteq.sga.repository;

import ec.edu.uteq.sga.entity.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    List<Auditoria> findByUsuario_IdUsuario(Long idUsuario);
    List<Auditoria> findByTablaAfectada(String tablaAfectada);
    List<Auditoria> findByAccion(String accion);
    List<Auditoria> findByTraceIdOrderByFechaAsc(UUID traceId);

    /**
     * Nativa (no JPQL): accion es un enum nativo de Postgres, y a diferencia
     * del INSERT/UPDATE (donde @ColumnTransformer en la entidad si resuelve
     * el cast), una comparacion JPQL "a.accion = :accion" sigue enviando el
     * parametro como varchar sin tipo -- Postgres no tiene el operador "="
     * entre accion_auditoria_t y varchar y lo rechaza. Con SQL nativo se
     * puede castear el parametro explicitamente.
     */
    @Query(value = """
            SELECT * FROM sga_principal.auditoria WHERE
            (:schemaOrigen IS NULL OR schema_origen = :schemaOrigen) AND
            (:accion IS NULL OR accion = CAST(:accion AS sga_principal.accion_auditoria_t)) AND
            (:tablaAfectada IS NULL OR tabla_afectada = :tablaAfectada) AND
            (:resultado IS NULL OR resultado = :resultado) AND
            (:username IS NULL OR username = :username)
            ORDER BY fecha DESC
            """,
            countQuery = """
            SELECT count(*) FROM sga_principal.auditoria WHERE
            (:schemaOrigen IS NULL OR schema_origen = :schemaOrigen) AND
            (:accion IS NULL OR accion = CAST(:accion AS sga_principal.accion_auditoria_t)) AND
            (:tablaAfectada IS NULL OR tabla_afectada = :tablaAfectada) AND
            (:resultado IS NULL OR resultado = :resultado) AND
            (:username IS NULL OR username = :username)
            """,
            nativeQuery = true)
    Page<Auditoria> buscar(@Param("schemaOrigen") String schemaOrigen,
                            @Param("accion") String accion,
                            @Param("tablaAfectada") String tablaAfectada,
                            @Param("resultado") String resultado,
                            @Param("username") String username,
                            Pageable pageable);
}