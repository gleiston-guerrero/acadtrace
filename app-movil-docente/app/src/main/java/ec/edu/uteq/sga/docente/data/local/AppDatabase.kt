package ec.edu.uteq.sga.docente.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ec.edu.uteq.sga.docente.core.Constants
import ec.edu.uteq.sga.docente.data.local.dao.*
import ec.edu.uteq.sga.docente.data.local.entity.*

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
        PendingSyncEntity::class
    ],
    version = 2,
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
