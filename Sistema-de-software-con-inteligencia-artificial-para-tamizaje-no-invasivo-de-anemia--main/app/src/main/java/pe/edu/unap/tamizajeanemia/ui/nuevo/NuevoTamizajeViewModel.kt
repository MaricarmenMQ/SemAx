package pe.edu.unap.tamizajeanemia.ui.nuevo

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pe.edu.unap.tamizajeanemia.clinico.Hemoglobina
import pe.edu.unap.tamizajeanemia.clinico.Sexo
import pe.edu.unap.tamizajeanemia.datos.RepositorioTamizajes
import pe.edu.unap.tamizajeanemia.datos.Tamizaje
import pe.edu.unap.tamizajeanemia.ia.ClasificadorAnemia
import pe.edu.unap.tamizajeanemia.ia.Modalidad
import java.io.File

class NuevoTamizajeViewModel(
    private val repositorio: RepositorioTamizajes,
    private val clasificador: () -> ClasificadorAnemia,
) : ViewModel() {

    var codigo by mutableStateOf("")
    var edadMeses by mutableStateOf("")
    var sexo by mutableStateOf<Sexo?>(null)
    var altitud by mutableStateOf(Hemoglobina.ALTITUD_PUNO_MSNM.toString())
    var hb by mutableStateOf("")

    val fotos = mutableStateMapOf<Modalidad, File>()
    var analizando by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun reiniciar() {
        codigo = ""
        edadMeses = ""
        sexo = null
        altitud = Hemoglobina.ALTITUD_PUNO_MSNM.toString()
        hb = ""
        fotos.clear()
        error = null
    }

    /** Devuelve el primer problema del formulario, o null si está completo. */
    fun validarDatos(): String? {
        val edad = edadMeses.toIntOrNull()
        val alt = altitud.toIntOrNull()
        return when {
            codigo.isBlank() -> "Ingrese el código del participante."
            edad == null || edad !in Hemoglobina.EDAD_MIN_MESES..Hemoglobina.EDAD_MAX_MESES ->
                "La edad debe estar entre ${Hemoglobina.EDAD_MIN_MESES} y ${Hemoglobina.EDAD_MAX_MESES} meses."
            sexo == null -> "Seleccione el sexo."
            alt == null || alt !in 0..5500 -> "Ingrese una altitud válida en m s. n. m."
            hb.isNotBlank() && numero(hb)?.takeIf { it in 3.0..25.0 } == null ->
                "La hemoglobina debe estar entre 3 y 25 g/dL, o dejarse vacía."
            else -> null
        }
    }

    fun nuevoArchivo(modalidad: Modalidad): File = repositorio.nuevoArchivoFoto(modalidad)

    fun guardarFoto(modalidad: Modalidad, archivo: File) {
        fotos[modalidad]?.takeIf { it != archivo }?.delete()
        fotos[modalidad] = archivo
    }

    fun analizarYGuardar(onGuardado: (Long) -> Unit) {
        if (analizando || fotos.size < Modalidad.entries.size) return
        analizando = true
        error = null
        viewModelScope.launch {
            try {
                val id = withContext(Dispatchers.Default) {
                    val ia = clasificador()
                    val resultado = ia.clasificar(fotos.toMap())
                    repositorio.guardar(
                        Tamizaje(
                            fechaMillis = System.currentTimeMillis(),
                            codigoParticipante = codigo.trim(),
                            edadMeses = edadMeses.toInt(),
                            sexo = sexo!!,
                            altitudMsnm = altitud.toInt(),
                            hbObservada = numero(hb),
                            rutaConjuntiva = fotos.getValue(Modalidad.CONJUNTIVA).path,
                            rutaPalma = fotos.getValue(Modalidad.PALMA).path,
                            rutaUna = fotos.getValue(Modalidad.UNA).path,
                            probabilidadIa = resultado.probabilidad,
                            probConjuntiva = resultado.porModalidad[Modalidad.CONJUNTIVA],
                            probPalma = resultado.porModalidad[Modalidad.PALMA],
                            probUna = resultado.porModalidad[Modalidad.UNA],
                            modeloIa = resultado.modelo,
                            iaSimulada = resultado.simulado,
                        ),
                    )
                }
                onGuardado(id)
            } catch (e: Exception) {
                error = "No se pudo analizar: ${e.message}"
            } finally {
                analizando = false
            }
        }
    }

    private fun numero(texto: String) = texto.trim().replace(',', '.').toDoubleOrNull()
}
