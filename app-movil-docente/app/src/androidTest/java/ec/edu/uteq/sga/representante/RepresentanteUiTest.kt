package ec.edu.uteq.sga.representante

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import ec.edu.uteq.sga.representante.ui.screens.representante.HomeRepresentante
import org.junit.Rule
import org.junit.Test

class RepresentanteUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeMuestraOpcionesExclusivasDelRepresentante() {
        compose.setContent { HomeRepresentante({}, {}, {}, {}) }
        compose.onNodeWithText("Mis representados").assertIsDisplayed()
        compose.onNodeWithText("Comunicados").assertIsDisplayed()
        compose.onNodeWithText("Seguridad").assertIsDisplayed()
    }
}
