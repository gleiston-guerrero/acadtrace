package ec.edu.uteq.sga.representante.data.sync

object RepresentanteSyncPolicy {
    fun acceptsAcademicWrite(@Suppress("UNUSED_PARAMETER") entityType: String): Boolean = false
    fun shouldRetry(networkAvailable: Boolean, refreshCompleted: Boolean): Boolean =
        !networkAvailable || !refreshCompleted
}
