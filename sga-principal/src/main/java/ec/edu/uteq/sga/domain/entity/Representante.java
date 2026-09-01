package ec.edu.uteq.sga.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "representantes", schema = "sga_principal")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Representante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_representante")
    private Long idRepresentante;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", unique = true)
    private Usuario usuario;

    @Column(unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(length = 50)
    private String parentesco;

    @Column(name = "telefono_principal", nullable = false, length = 20)
    private String telefonoPrincipal;

    @Column(name = "telefono_alt", length = 20)
    private String telefonoAlt;

    @Column(length = 100)
    private String correo;

    @Column(columnDefinition = "text")
    private String direccion;

    @Column(name = "fecha_nacimiento")
    private java.time.LocalDate fechaNacimiento;

    @Column(length = 20)
    private String genero;

    @Column(name = "estado_civil", length = 30)
    private String estadoCivil;

    @Column(length = 50)
    private String nacionalidad;

    @Column(length = 100)
    private String ocupacion;

    @Column(name = "lugar_trabajo", length = 150)
    private String lugarTrabajo;

    @Column(name = "telefono_trabajo", length = 20)
    private String telefonoTrabajo;

    @Column(length = 100)
    private String cargo;

    @Column(name = "nivel_instruccion", length = 50)
    private String nivelInstruccion;

    @Column(name = "ingreso_mensual", precision = 10, scale = 2)
    private java.math.BigDecimal ingresoMensual;

    @Column(name = "convive_con_estudiante")
    private Boolean conviveConEstudiante;

    @Column(name = "contacto_emergencia_nombre", length = 150)
    private String contactoEmergenciaNombre;

    @Column(name = "contacto_emergencia_telefono", length = 20)
    private String contactoEmergenciaTelefono;

    @Column(columnDefinition = "text")
    private String observaciones;

    @Column(name = "fecha_creacion")
    private Instant fechaCreacion = Instant.now();
}
