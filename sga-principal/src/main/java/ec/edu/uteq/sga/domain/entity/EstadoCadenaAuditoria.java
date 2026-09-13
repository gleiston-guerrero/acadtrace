package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cabeza singleton de la cadena global de auditoria.
 *
 * La fila id_estado = 1 se bloquea con PESSIMISTIC_WRITE antes de
 * calcular cada nuevo eslabon para impedir bifurcaciones concurrentes.
 */
@Entity
@Table(name = "estado_cadena_auditoria", schema = "sga_principal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoCadenaAuditoria {

    @Id
    @Column(name = "id_estado")
    private Short idEstado;

    @Column(name = "ultimo_hash", nullable = false, length = 64)
    private String ultimoHash;

    @Column(name = "ultimo_lamport", nullable = false)
    private Long ultimoLamport;

    @Column(name = "vector_reloj", columnDefinition = "text")
    private String vectorReloj;
}
