package ec.edu.uteq.sga.docente.core

object Constants {
    // URL base por defecto para el Gateway/Principal (puerto 8080 en emulador: 10.0.2.2)
    const val DEFAULT_BASE_GATEWAY_URL = "http://10.0.2.2:8080/api/"
    
    // URL base por defecto para el Microservicio Docente (puerto 8081 en emulador: 10.0.2.2)
    const val DEFAULT_BASE_DOCENTE_URL = "http://10.0.2.2:8081/api/"

    const val PREFS_NAME = "sga_docente_secure_prefs"
    const val KEY_JWT_TOKEN = "jwt_token"
    const val KEY_USER_ID = "user_id"
    const val KEY_USERNAME = "username"
    const val KEY_CORREO = "correo"
    const val KEY_ROLES = "roles"
    const val KEY_ID_PERSONA = "id_persona"
    const val KEY_GATEWAY_URL = "gateway_url"
    const val KEY_DOCENTE_URL = "docente_url"

    const val DATABASE_NAME = "sga_docente_database.db"

    // Roles permitidos
    const val ROLE_DOCENTE = "DOCENTE"
}
