package pe.edu.unap.tamizajeanemia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pe.edu.unap.tamizajeanemia.ui.NavegacionApp
import pe.edu.unap.tamizajeanemia.ui.tema.TemaTamizaje

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val contenedor = (application as TamizajeApp).contenedor
        setContent {
            TemaTamizaje {
                NavegacionApp(contenedor)
            }
        }
    }
}
