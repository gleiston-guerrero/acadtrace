package ec.edu.uteq.sga.representante.core

object Constants {
    // URL base por defecto para el Gateway/Principal (puerto 8080)
    const val DEFAULT_BASE_GATEWAY_URL = "http://10.0.2.2:8080/api/"
    
    // URL base por defecto para el Microservicio Docente (puerto 8081)
    const val DEFAULT_BASE_DOCENTE_URL = "http://10.0.2.2:8081/api/docente/"

    const val PREFS_NAME = "sga_representante_secure_prefs"
    const val KEY_JWT_TOKEN = "jwt_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_CORREO = "correo"
    const val KEY_ROLES = "roles"
    const val KEY_PRIMER_INGRESO = "primer_ingreso"
    const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
    const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
    const val KEY_ID_PERSONA = "id_persona"
    const val KEY_GATEWAY_URL = "gateway_url"
    const val KEY_DOCENTE_URL = "docente_url"

    const val DATABASE_NAME = "sga_representante_database.db"

    // Roles permitidos
    const val ROL_REPRESENTANTE_LOGIN = "REPRESENTANTE"
}
