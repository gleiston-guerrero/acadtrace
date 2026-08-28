package ec.edu.uteq.sga.infrastructure.repository;

import ec.edu.uteq.sga.domain.entity.Auditoria;
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
    /**
     * categoria agrupa varias acciones para las secciones del submenu del
     * lado del frontend (CRUD/ACCESOS/CONFIG/GRPC): comparando CAST(accion AS text)
     * en vez del enum se evita el problema de cast de mas arriba sin
     * necesitar una lista dinamica de parametros.
     */
    @Query(value = """
            SELECT * FROM sga_principal.auditoria WHERE
            (:schemaOrigen IS NULL OR schema_origen = :schemaOrigen) AND
            (:accion IS NULL OR accion = CAST(:accion AS sga_principal.accion_auditoria_t)) AND
            (:categoria IS NULL OR
                (:categoria = 'CRUD' AND CAST(accion AS text) IN ('CREAR','EDITAR','ELIMINAR')) OR
                (:categoria = 'ACCESOS' AND CAST(accion AS text) IN ('LOGIN','LOGIN_FALLIDO','LOGOUT','CAMBIO_PASSWORD','BLOQUEO','DESBLOQUEO')) OR
                (:categoria = 'CONFIG' AND CAST(accion AS text) IN ('ROL_ASIGNADO')) OR
                (:categoria = 'GRPC' AND CAST(accion AS text) IN ('LLAMADA_GRPC'))
            ) AND
            (:tablaAfectada IS NULL OR tabla_afectada = :tablaAfectada) AND
            (:resultado IS NULL OR resultado = :resultado) AND
            (:username IS NULL OR username = :username)
            ORDER BY fecha DESC
            """,
            countQuery = """
            SELECT count(*) FROM sga_principal.auditoria WHERE
            (:schemaOrigen IS NULL OR schema_origen = :schemaOrigen) AND
            (:accion IS NULL OR accion = CAST(:accion AS sga_principal.accion_auditoria_t)) AND
            (:categoria IS NULL OR
                (:categoria = 'CRUD' AND CAST(accion AS text) IN ('CREAR','EDITAR','ELIMINAR')) OR
                (:categoria = 'ACCESOS' AND CAST(accion AS text) IN ('LOGIN','LOGIN_FALLIDO','LOGOUT','CAMBIO_PASSWORD','BLOQUEO','DESBLOQUEO')) OR
                (:categoria = 'CONFIG' AND CAST(accion AS text) IN ('ROL_ASIGNADO')) OR
                (:categoria = 'GRPC' AND CAST(accion AS text) IN ('LLAMADA_GRPC'))
            ) AND
            (:tablaAfectada IS NULL OR tabla_afectada = :tablaAfectada) AND
            (:resultado IS NULL OR resultado = :resultado) AND
            (:username IS NULL OR username = :username)
            """,
            nativeQuery = true)
    Page<Auditoria> buscar(@Param("schemaOrigen") String schemaOrigen,
                            @Param("accion") String accion,
                            @Param("categoria") String categoria,
                            @Param("tablaAfectada") String tablaAfectada,
                            @Param("resultado") String resultado,
                            @Param("username") String username,
                            Pageable pageable);
}