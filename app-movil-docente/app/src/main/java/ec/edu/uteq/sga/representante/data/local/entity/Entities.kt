package ec.edu.uteq.sga.representante.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "representados_cache")
data class RepresentadoCacheEntity(@PrimaryKey val idEstudiante: Long, val json: String, val lastUpdated: Long = System.currentTimeMillis())

@Entity(tableName = "calificaciones_representado_cache")
data class CalificacionesRepresentadoCacheEntity(@PrimaryKey val idEstudiante: Long, val json: String, val lastUpdated: Long = System.currentTimeMillis())

@Entity(tableName = "asistencia_hijo_cache")
data class AsistenciaHijoCacheEntity(@PrimaryKey val idEstudiante: Long, val json: String, val lastUpdated: Long = System.currentTimeMillis())

@Entity(
    tableName = "asignaciones",
    indices = [
        Index(value = ["asignaturaId"]),
        Index(value = ["gradoId"]),
        Index(value = ["paraleloId"]),
        Index(value = ["anoLectivoId"])
    ]
)
data class AsignacionEntity(
    @PrimaryKey val idAsignacion: Long,
    val asignaturaId: Long,
    val asignaturaNombre: String,
    val gradoId: Long,
    val gradoNombre: String,
    val paraleloId: Long,
    val paraleloLetra: String,
    val anoLectivoId: Long,
    val anoLectivoNombre: String,
    val cantidadEstudiantes: Int = 0,
    val porcentajeAsistencia: Double? = null,
    val promedioCalificaciones: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "estudiantes",
    primaryKeys = ["idMatricula", "idAsignacion"],
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["estudianteId"]),
        Index(value = ["cedula"])
    ]
)
data class EstudianteEntity(
    val idMatricula: Long,
    val idAsignacion: Long,
    val estudianteId: Long,
    val nombres: String,
    val apellidos: String,
    val cedula: String = "",
    val estadoMatricula: String = "ACTIVA",
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "periodos",
    indices = [
        Index(value = ["idAnoLectivo"]),
        Index(value = ["activo"])
    ]
)
data class PeriodoEntity(
    @PrimaryKey val idPeriodo: Long,
    val idAnoLectivo: Long,
    val tipo: String,
    val nombre: String,
    val fechaInicio: String,
    val fechaFin: String,
    val activo: Boolean = true
)

@Entity(
    tableName = "actividades",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["idPeriodo"]),
        Index(value = ["idAsignacion", "idPeriodo"]),
        Index(value = ["isPendingSync"])
    ]
)
data class ActividadEntity(
    @PrimaryKey val idActividad: Long, // Puede ser id temporal negativo para creadas offline
    val idAsignacion: Long,
    val idPeriodo: Long,
    val tipo: String,
    val nombre: String,
    val descripcion: String? = null,
    val fechaEntrega: String,
    val ponderacion: Double = 0.0,
    val notaMaxima: Double = 10.0,
    val esSumativa: Boolean = false,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "calificaciones",
    indices = [
        Index(value = ["idActividad"]),
        Index(value = ["idMatricula"]),
        Index(value = ["idActividad", "idMatricula"]),
        Index(value = ["isPendingSync"])
    ]
)
data class CalificacionEntity(
    @PrimaryKey val idCalificacion: Long, // Id temporal negativo si es offline
    val idActividad: Long,
    val idMatricula: Long,
    val nota: Double,
    val notaCualitativa: String? = null,
    val observacion: String? = null,
    val registradoPor: Long? = null,
    val fechaRegistro: String? = null,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "asistencias",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["idMatricula"]),
        Index(value = ["idAsignacion", "fecha"]),
        Index(value = ["idMatricula", "idAsignacion", "fecha"]),
        Index(value = ["isPendingSync"])
    ]
)
data class AsistenciaEntity(
    @PrimaryKey val idAsistencia: Long, // Id temporal negativo si es offline
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val fecha: String,
    val estado: String, // PRESENTE, AUSENTE, JUSTIFICADO, ATRASO
    val justificacion: String? = null,
    val registradoPor: Long? = null,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "resumenes_asistencia",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["idMatricula"]),
        Index(value = ["idAsignacion", "idPeriodo"])
    ]
)
data class ResumenAsistenciaEntity(
    @PrimaryKey val idResumen: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val totalPresentes: Int = 0,
    val totalAusentes: Int = 0,
    val totalJustificados: Int = 0,
    val totalAtrasos: Int = 0,
    val calculadoEn: String? = null
)

@Entity(
    tableName = "promedios_trimestrales",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["idMatricula"]),
        Index(value = ["idAsignacion", "idPeriodo"])
    ]
)
data class PromedioTrimestralEntity(
    @PrimaryKey val idPromedio: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idPeriodo: Long,
    val promedioFormativo: Double = 0.0,
    val notaSumativa: Double = 0.0,
    val promedioTrimestral: Double = 0.0,
    val notaCualitativa: String = "D",
    val calculadoEn: String? = null
)

@Entity(
    tableName = "promedios_anuales",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["idMatricula"]),
        Index(value = ["idAsignacion", "idAnoLectivo"])
    ]
)
data class PromedioAnualEntity(
    @PrimaryKey val idPromedioAnual: Long,
    val idMatricula: Long,
    val idAsignacion: Long,
    val idAnoLectivo: Long,
    val promedioAnual: Double = 0.0,
    val notaCualitativa: String = "D",
    val registradoPor: Long? = null,
    val calculadoEn: String? = null
)

@Entity(
    tableName = "seguimiento_academico",
    indices = [
        Index(value = ["idMatricula"]),
        Index(value = ["idPeriodo"]),
        Index(value = ["fechaEvento"]),
        Index(value = ["isPendingSync"])
    ]
)
data class SeguimientoEntity(
    @PrimaryKey val idSeguimiento: Long,
    val idMatricula: Long,
    val idPeriodo: Long,
    val categoria: String,
    val descripcion: String,
    val accionesTomadas: String? = null,
    val requiereFollowup: Boolean = false,
    val fechaEvento: String,
    val registradoPor: Long? = null,
    val fechaRegistro: String? = null,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "anuncios",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["fijado"]),
        Index(value = ["fecha"]),
        Index(value = ["isPendingSync"])
    ]
)
data class AnuncioEntity(
    @PrimaryKey val idAnuncio: Long,
    val idAsignacion: Long,
    val titulo: String?,
    val contenido: String?,
    val autorId: Long? = null,
    val fecha: String? = null,
    val fijado: Boolean = false,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "materiales",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["fecha"]),
        Index(value = ["isPendingSync"])
    ]
)
data class MaterialEntity(
    @PrimaryKey val idMaterial: Long,
    val idAsignacion: Long,
    val tipo: String? = null,
    val titulo: String?,
    val descripcion: String? = null,
    val url: String,
    val tamanoBytes: Long? = null,
    val fecha: String? = null,
    val isPendingSync: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "horarios",
    indices = [
        Index(value = ["idAsignacion"]),
        Index(value = ["diaSemana"])
    ]
)
data class HorarioEntity(
    @PrimaryKey val idHorario: Long,
    val idAsignacion: Long,
    val diaSemana: Int,
    val idPeriodo: Int,
    val horaInicio: String,
    val horaFin: String,
    val aula: String? = null,
    val asignatura: String,
    val docente: String,
    val grado: String,
    val paralelo: String
)

// ─── COLA DE SINCRONIZACIÓN OFFLINE ────────────────────────────────────
@Entity(
    tableName = "pending_sync",
    indices = [
        Index(value = ["entityType"]),
        Index(value = ["actionType"]),
        Index(value = ["createdAt"])
    ]
)
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String, // ACTIVIDAD, CALIFICACION, ASISTENCIA, SEGUIMIENTO, ANUNCIO, MATERIAL
    val actionType: String, // CREATE, UPDATE, DELETE
    val localId: Long,
    val remoteId: Long? = null,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val attempts: Int = 0,
    val lastError: String? = null
)
