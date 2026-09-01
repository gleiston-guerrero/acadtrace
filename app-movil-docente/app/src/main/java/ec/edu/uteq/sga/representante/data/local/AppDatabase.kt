package ec.edu.uteq.sga.representante.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ec.edu.uteq.sga.representante.core.Constants
import ec.edu.uteq.sga.representante.data.local.dao.*
import ec.edu.uteq.sga.representante.data.local.entity.*

@Database(
    entities = [
        AsignacionEntity::class,
        EstudianteEntity::class,
        PeriodoEntity::class,
        ActividadEntity::class,
        CalificacionEntity::class,
        AsistenciaEntity::class,
        ResumenAsistenciaEntity::class,
        PromedioTrimestralEntity::class,
        PromedioAnualEntity::class,
        SeguimientoEntity::class,
        AnuncioEntity::class,
        MaterialEntity::class,
        HorarioEntity::class,
        PendingSyncEntity::class,
        RepresentadoCacheEntity::class,
        CalificacionesRepresentadoCacheEntity::class,
        AsistenciaHijoCacheEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun asignacionDao(): AsignacionDao
    abstract fun estudianteDao(): EstudianteDao
    abstract fun periodoDao(): PeriodoDao
    abstract fun actividadDao(): ActividadDao
    abstract fun calificacionDao(): CalificacionDao
    abstract fun asistenciaDao(): AsistenciaDao
    abstract fun resumenAsistenciaDao(): ResumenAsistenciaDao
    abstract fun promediosDao(): PromediosDao
    abstract fun seguimientoDao(): SeguimientoDao
    abstract fun anuncioDao(): AnuncioDao
    abstract fun materialDao(): MaterialDao
    abstract fun horarioDao(): HorarioDao
    abstract fun pendingSyncDao(): PendingSyncDao
    abstract fun representanteCacheDao(): RepresentanteCacheDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                .addMigrations(MIGRATION_3_4)
                .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS representados_cache (idEstudiante INTEGER NOT NULL, json TEXT NOT NULL, lastUpdated INTEGER NOT NULL, PRIMARY KEY(idEstudiante))")
                db.execSQL("CREATE TABLE IF NOT EXISTS calificaciones_representado_cache (idEstudiante INTEGER NOT NULL, json TEXT NOT NULL, lastUpdated INTEGER NOT NULL, PRIMARY KEY(idEstudiante))")
                db.execSQL("CREATE TABLE IF NOT EXISTS asistencia_hijo_cache (idEstudiante INTEGER NOT NULL, json TEXT NOT NULL, lastUpdated INTEGER NOT NULL, PRIMARY KEY(idEstudiante))")
            }
        }
    }
}
