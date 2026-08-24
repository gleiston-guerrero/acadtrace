package ec.edu.uteq.sga.docente.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── AUTH ─────────────────────────────────────────────────────────────
data class LoginRequest(
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("idUsuario") val idUsuario: Long,
    @SerializedName("username") val username: String,
    @SerializedName("correo") val correo: String?,
    @SerializedName("roles") val roles: List<String>,
    @SerializedName("primerIngreso") val primerIngreso: Boolean = false
)

// ─── DOCENTE CONTEXT ──────────────────────────────────────────────────
data class IdNombreDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("nombre") val nombre: String
)

data class ParaleloDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("letra") val letra: String
)

data class AsignacionDocenteDTO(
    @SerializedName("idAsignacion") val idAsignacion: Long,
    @SerializedName("asignatura") val asignatura: IdNombreDTO,
    @SerializedName("grado") val grado: IdNombreDTO,
    @SerializedName("paralelo") val paralelo: ParaleloDTO,
    @SerializedName("anoLectivo") val anoLectivo: IdNombreDTO,
    @SerializedName("cantidadEstudiantes") val cantidadEstudiantes: Int = 0,
    @SerializedName("porcentajeAsistencia") val porcentajeAsistencia: Double? = null,
    @SerializedName("promedioCalificaciones") val promedioCalificaciones: Double? = null
)

data class EstudianteInfoDTO(
    @SerializedName("id") val id: Long,
    @SerializedName("nombres") val nombres: String,
    @SerializedName("apellidos") val apellidos: String,
    @SerializedName("cedula") val cedula: String? = ""
)

data class MatriculaEstudianteDTO(
    @SerializedName("idMatricula") val idMatricula: Long,
    @SerializedName("estudiante") val estudiante: EstudianteInfoDTO,
    @SerializedName("estado") val estado: String? = "ACTIVA"
)

data class AnoLectivoDTO(
    @SerializedName("idAnoLectivo") val idAnoLectivo: Long,
    @SerializedName("nombre") val nombre: String
)
