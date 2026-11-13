package pe.edu.unap.tamizajeanemia.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pe.edu.unap.tamizajeanemia.Contenedor
import pe.edu.unap.tamizajeanemia.ui.inicio.PantallaInicio
import pe.edu.unap.tamizajeanemia.ui.nuevo.NuevoTamizajeViewModel
import pe.edu.unap.tamizajeanemia.ui.nuevo.PantallaCaptura
import pe.edu.unap.tamizajeanemia.ui.nuevo.PantallaDatos
import pe.edu.unap.tamizajeanemia.ui.resultado.PantallaResultado

@Composable
fun NavegacionApp(contenedor: Contenedor) {
    val nav = rememberNavController()
    // Un solo borrador de tamizaje compartido por las pantallas de datos y captura.
    val borrador: NuevoTamizajeViewModel = viewModel(
        factory = viewModelFactory {
            initializer { NuevoTamizajeViewModel(contenedor.repositorio) { contenedor.clasificador } }
        },
    )

    val nuevoTamizaje = {
        borrador.reiniciar()
        nav.navigate("datos") { popUpTo("inicio") }
    }

    NavHost(navController = nav, startDestination = "inicio") {
        composable("inicio") {
            PantallaInicio(
                tamizajes = remember { contenedor.repositorio.todos() },
                estadoIa = remember { contenedor.clasificador.descripcion },
                onNuevo = nuevoTamizaje,
                onAbrir = { id -> nav.navigate("resultado/$id") },
                exportar = contenedor.repositorio::exportar,
            )
        }
        composable("datos") {
            PantallaDatos(
                vm = borrador,
                onVolver = { nav.popBackStack() },
                onContinuar = { nav.navigate("captura") },
            )
        }
        composable("captura") {
            PantallaCaptura(
                vm = borrador,
                onVolver = { nav.popBackStack() },
                onGuardado = { id ->
                    nav.navigate("resultado/$id") { popUpTo("inicio") }
                },
            )
        }
        composable(
            "resultado/{id}",
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entrada ->
            val id = entrada.arguments!!.getLong("id")
            PantallaResultado(
                tamizaje = remember(id) { contenedor.repositorio.porId(id) },
                onVolver = { nav.popBackStack("inicio", inclusive = false) },
                onNuevo = nuevoTamizaje,
            )
        }
    }
}
