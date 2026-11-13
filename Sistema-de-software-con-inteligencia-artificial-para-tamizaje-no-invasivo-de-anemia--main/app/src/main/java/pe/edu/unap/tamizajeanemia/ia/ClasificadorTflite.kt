package pe.edu.unap.tamizajeanemia.ia

import android.content.Context
import android.graphics.Bitmap
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.exp

/**
 * Clasifica cada foto con el modelo .tflite de su modalidad (`modelo/conjuntiva.tflite`,
 * `modelo/palma.tflite`, `modelo/una.tflite`) y combina las probabilidades con un promedio
 * ponderado por la calidad de cada modelo (AUC − 0,5). Es una fusión provisional: los
 * datasets actuales no permiten validarla porque no emparejan fotos del mismo niño.
 * Las modalidades sin modelo se guardan pero no se clasifican.
 */
class ClasificadorTflite(private val modelos: Map<Modalidad, ModeloModalidad>) : ClasificadorAnemia {
    override val nombre = modelos.values.joinToString(" + ") { it.version }
    override val simulado = false
    override val descripcion =
        "IA provisional sin conexión · " + modelos.keys.joinToString(", ") { it.corto.lowercase() }

    @Synchronized
    override fun clasificar(fotos: Map<Modalidad, File>): ResultadoIa {
        val porModalidad = fotos.mapNotNull { (modalidad, archivo) ->
            val modelo = modelos[modalidad] ?: return@mapNotNull null
            modelo.clasificar(archivo)?.let { modalidad to it }
        }.toMap()
        val pesoTotal = porModalidad.keys.sumOf { modelos.getValue(it).peso }
        return ResultadoIa(
            probabilidad = (porModalidad.entries.sumOf { (m, p) -> modelos.getValue(m).peso * p } / pesoTotal).toFloat(),
            porModalidad = porModalidad,
            modelo = nombre,
            simulado = false,
        )
    }
}

/**
 * Un modelo de clasificación de imágenes (p. ej. MobileNetV3/EfficientNetB0) para una modalidad.
 *
 * Supuestos, a mantener al exportar los modelos del proyecto:
 * - entrada [1, alto, ancho, 3] float32 con píxeles RGB normalizados a 0–1;
 * - salida [1, 1] (probabilidad de anemia) o [1, 2] (logits/probabilidades: sin anemia, anemia).
 *
 * Junto al .tflite va un .json con `version` (nombre que se registra) y `recorte_central`
 * (ancho, alto en fracción de la foto: zona del recuadro guía que se recorta antes de clasificar).
 */
class ModeloModalidad(context: Context, rutaAsset: String) {
    val version: String

    /** Peso en la fusión: AUC de prueba − 0,5 (1 si el .json no trae AUC). */
    val peso: Double
    private val recorte: Pair<Float, Float>?
    private val interprete: Interpreter
    private val alto: Int
    private val ancho: Int
    private val salidas: Int

    init {
        val config = runCatching {
            JSONObject(context.assets.open(rutaAsset.replace(".tflite", ".json")).bufferedReader().readText())
        }.getOrNull()
        version = config?.optString("version")?.takeIf { it.isNotBlank() } ?: rutaAsset.substringAfterLast('/')
        peso = config?.optDouble("auc_prueba")?.takeIf { !it.isNaN() }?.let { (it - 0.5).coerceAtLeast(0.05) } ?: 1.0
        recorte = config?.optJSONArray("recorte_central")
            ?.let { it.getDouble(0).toFloat() to it.getDouble(1).toFloat() }

        val descriptor = context.assets.openFd(rutaAsset)
        val modelo = FileInputStream(descriptor.fileDescriptor).channel.map(
            FileChannel.MapMode.READ_ONLY, descriptor.startOffset, descriptor.declaredLength,
        )
        interprete = Interpreter(modelo, Interpreter.Options().setNumThreads(4))
        val forma = interprete.getInputTensor(0).shape()
        alto = forma[1]
        ancho = forma[2]
        salidas = interprete.getOutputTensor(0).shape().last()
    }

    fun clasificar(archivo: File): Float? {
        var bitmap = decodificarReducida(archivo, maxOf(alto, ancho) * 4) ?: return null
        recorte?.let { (fAncho, fAlto) ->
            val w = (bitmap.width * fAncho).toInt()
            val h = (bitmap.height * fAlto).toInt()
            bitmap = Bitmap.createBitmap(bitmap, (bitmap.width - w) / 2, (bitmap.height - h) / 2, w, h)
        }
        return inferir(Bitmap.createScaledBitmap(bitmap, ancho, alto, true))
    }

    private fun inferir(bitmap: Bitmap): Float {
        val entrada = ByteBuffer.allocateDirect(4 * alto * ancho * 3).order(ByteOrder.nativeOrder())
        val pixeles = IntArray(alto * ancho)
        bitmap.getPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        for (p in pixeles) {
            entrada.putFloat(((p shr 16) and 0xFF) / 255f)
            entrada.putFloat(((p shr 8) and 0xFF) / 255f)
            entrada.putFloat((p and 0xFF) / 255f)
        }
        val salida = Array(1) { FloatArray(salidas) }
        interprete.run(entrada, salida)
        val valores = salida[0]
        if (salidas == 1) return valores[0].coerceIn(0f, 1f)
        // Dos clases: si no suman ~1 son logits y se aplica softmax.
        if (valores.all { it in 0f..1f } && kotlin.math.abs(valores.sum() - 1f) < 0.01f) return valores[1]
        val e0 = exp(valores[0].toDouble())
        val e1 = exp(valores[1].toDouble())
        return (e1 / (e0 + e1)).toFloat()
    }
}
