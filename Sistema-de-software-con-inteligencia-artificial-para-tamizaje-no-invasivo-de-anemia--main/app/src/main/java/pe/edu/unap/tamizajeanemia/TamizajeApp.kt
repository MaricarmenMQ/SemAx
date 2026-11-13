package pe.edu.unap.tamizajeanemia

import android.app.Application
import pe.edu.unap.tamizajeanemia.datos.BaseDatos
import pe.edu.unap.tamizajeanemia.datos.RepositorioTamizajes
import pe.edu.unap.tamizajeanemia.ia.ClasificadorAnemia

class TamizajeApp : Application() {
    lateinit var contenedor: Contenedor
        private set

    override fun onCreate() {
        super.onCreate()
        contenedor = Contenedor(this)
    }
}

/** Dependencias compartidas de la app (inyección manual, suficiente para el prototipo). */
class Contenedor(app: Application) {
    val repositorio = RepositorioTamizajes(BaseDatos.crear(app).tamizajes(), app.filesDir)
    val clasificador: ClasificadorAnemia by lazy { ClasificadorAnemia.crear(app) }
}
