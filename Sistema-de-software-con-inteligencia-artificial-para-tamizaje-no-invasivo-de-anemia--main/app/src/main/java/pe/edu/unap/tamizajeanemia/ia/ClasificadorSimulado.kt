package pe.edu.unap.tamizajeanemia.ia

import java.io.File

/**
 * Sustituto de la demo mientras no exista un modelo entrenado. NO tiene valor clínico:
 * estima una "probabilidad" a partir de cuán rojiza es la imagen, solo para que el flujo
 * completo (captura → análisis → registro) funcione. La interfaz lo marca como simulado.
 */
class ClasificadorSimulado : ClasificadorAnemia {
    override val nombre = "simulado"
    override val simulado = true
    override val descripcion = "IA en modo demo: resultados simulados"

    override fun clasificar(fotos: Map<Modalidad, File>): ResultadoIa {
        val porModalidad = fotos.mapNotNull { (modalidad, archivo) ->
            val bitmap = decodificarReducida(archivo, 64) ?: return@mapNotNull null
            val pixeles = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixeles, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            var rojo = 0.0
            var total = 0.0
            for (p in pixeles) {
                val r = (p shr 16) and 0xFF
                rojo += r
                total += r + ((p shr 8) and 0xFF) + (p and 0xFF)
            }
            val proporcionRoja = if (total == 0.0) 1.0 / 3 else rojo / total
            // Más pálido (menos rojo) → mayor "probabilidad" simulada.
            modalidad to (1.0 - (proporcionRoja - 0.30) / 0.20).coerceIn(0.0, 1.0).toFloat()
        }.toMap()
        return ResultadoIa(
            probabilidad = porModalidad.values.average().toFloat(),
            porModalidad = porModalidad,
            modelo = nombre,
            simulado = true,
        )
    }
}
