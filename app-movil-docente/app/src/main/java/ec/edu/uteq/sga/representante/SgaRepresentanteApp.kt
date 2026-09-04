package ec.edu.uteq.sga.representante

import android.app.Application
import ec.edu.uteq.sga.representante.core.SessionManager
import ec.edu.uteq.sga.representante.data.local.AppDatabase
import ec.edu.uteq.sga.representante.data.remote.RetrofitClient
import ec.edu.uteq.sga.representante.data.repository.AuthRepositoryImpl
import ec.edu.uteq.sga.representante.data.repository.RepresentanteRepositoryImpl
import ec.edu.uteq.sga.representante.domain.repository.AuthRepository
import ec.edu.uteq.sga.representante.domain.repository.RepresentanteRepository
import ec.edu.uteq.sga.representante.notifications.NotificationSupport
import ec.edu.uteq.sga.representante.data.sync.SyncWorker

class SgaRepresentanteApp : Application() {
    lateinit var database: AppDatabase private set
    lateinit var sessionManager: SessionManager private set
    lateinit var authRepository: AuthRepository private set
    lateinit var representanteRepository: RepresentanteRepository private set
    override fun onCreate() {
        super.onCreate()
        sessionManager = SessionManager(this)
        database = AppDatabase.getInstance(this)
        val client = RetrofitClient(sessionManager)
        authRepository = AuthRepositoryImpl(client, sessionManager)
        representanteRepository = RepresentanteRepositoryImpl(database, client)
        NotificationSupport.createChannel(this)
        if (sessionManager.areNotificationsEnabled()) NotificationSupport.schedule(this)
        SyncWorker.schedulePeriodicSync(this)
    }
}
