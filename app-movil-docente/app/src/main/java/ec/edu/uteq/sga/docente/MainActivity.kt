package ec.edu.uteq.sga.docente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import ec.edu.uteq.sga.docente.ui.navigation.Screen
import ec.edu.uteq.sga.docente.ui.navigation.SgaNavGraph
import ec.edu.uteq.sga.docente.ui.theme.SgaDocenteAppTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as SgaDocenteApp
        val startDestination = if (app.authRepository.isUserLoggedIn()) {
            Screen.Dashboard.route
        } else {
            Screen.Login.route
        }

        setContent {
            SgaDocenteAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    SgaNavGraph(
                        navController = navController,
                        app = app,
                        startDestination = startDestination
                    )
                }
            }
        }
    }
}
