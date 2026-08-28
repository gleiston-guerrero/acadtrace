package ec.edu.uteq.sga.docente

import android.app.Application
import ec.edu.uteq.sga.docente.core.NetworkConnectivityObserver
import ec.edu.uteq.sga.docente.core.SessionManager
import ec.edu.uteq.sga.docente.data.local.AppDatabase
import ec.edu.uteq.sga.docente.data.remote.RetrofitClient
import ec.edu.uteq.sga.docente.data.repository.*
import ec.edu.uteq.sga.docente.data.sync.SyncManager
import ec.edu.uteq.sga.docente.data.sync.SyncWorker
import ec.edu.uteq.sga.docente.domain.repository.*

class SgaDocenteApp : Application() {

    lateinit var database: AppDatabase private set
    lateinit var sessionManager: SessionManager private set
    lateinit var connectivityObserver: NetworkConnectivityObserver private set
    lateinit var retrofitClient: RetrofitClient private set
    lateinit var syncManager: SyncManager private set

    // Repositorios
    lateinit var authRepository: AuthRepository private set
    lateinit var docenteRepository: DocenteRepository private set
    lateinit var actividadesRepository: ActividadesRepository private set
    lateinit var calificacionesRepository: CalificacionesRepository private set
    lateinit var asistenciasRepository: AsistenciasRepository private set
    lateinit var promediosRepository: PromediosRepository private set
    lateinit var seguimientoRepository: SeguimientoRepository private set
    lateinit var anunciosRepository: AnunciosRepository private set
    lateinit var materialesRepository: MaterialesRepository private set
    lateinit var aulaVirtualRepository: AulaVirtualRepository private set
    lateinit var horarioRepository: HorarioRepository private set

    override fun onCreate() {
        super.onCreate()

        sessionManager = SessionManager(this)
        database = AppDatabase.getInstance(this)
        connectivityObserver = NetworkConnectivityObserver(this)
        retrofitClient = RetrofitClient(sessionManager)
        syncManager = SyncManager(database, retrofitClient, connectivityObserver)

        // Inicializar repositorios
        authRepository = AuthRepositoryImpl(retrofitClient, sessionManager)
        docenteRepository = DocenteRepositoryImpl(database, retrofitClient, connectivityObserver)
        actividadesRepository = ActividadesRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        calificacionesRepository = CalificacionesRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        asistenciasRepository = AsistenciasRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        promediosRepository = PromediosRepositoryImpl(database, retrofitClient, connectivityObserver)
        seguimientoRepository = SeguimientoRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        anunciosRepository = AnunciosRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        materialesRepository = MaterialesRepositoryImpl(database, retrofitClient, syncManager, connectivityObserver)
        aulaVirtualRepository = AulaVirtualRepositoryImpl(retrofitClient)
        horarioRepository = HorariosRepositoryImpl(database, retrofitClient, connectivityObserver)

        // Programar sincronización periódica en segundo plano
        SyncWorker.schedulePeriodicSync(this)
    }
}
