package pe.edu.unap.tamizajeanemia.ia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File

/**
 * [guia]: recuadro (ancho, alto como fracción de la vista previa) donde debe quedar la zona a
 * analizar. Su tamaño en la foto lo define `recorte_central` en el .json del modelo.
 */
enum class Modalidad(val titulo: String, val instruccion: String, val guia: Pair<Float, Float>) {
    CONJUNTIVA(
        "Conjuntiva palpebral",
        "Baje suavemente el párpado inferior y ubique la parte interna rosada dentro del recuadro.",
        guia = 0.72f to 0.22f,
    ),
    PALMA(
        "Palma de la mano",
        "Extienda la mano con la palma hacia arriba y llene el recuadro con el centro de la palma, con buena luz.",
        guia = 0.60f to 0.30f,
    ),
    UNA(
        "Lecho ungueal",
        "Acerque la cámara y ubique una sola uña (sin esmalte ni suciedad) dentro del recuadro.",
        guia = 0.33f to 0.14f,
    ),
    ;

    /** Nombre del modelo en assets/modelo/ (sin extensión). */
    val archivo get() = name.lowercase()

    val corto get() = when (this) {
        CONJUNTIVA -> "Conjuntiva"
        PALMA -> "Palma"
        UNA -> "Uña"
    }
}

data class ResultadoIa(
    /** Probabilidad de anemia combinando las modalidades disponibles (0–1). */
    val probabilidad: Float,
    val porModalidad: Map<Modalidad, Float>,
    val modelo: String,
    val simulado: Boolean,
)

interface ClasificadorAnemia {
    val nombre: String
    val simulado: Boolean

    /** Texto corto para el usuario sobre qué IA está activa. */
    val descripcion: String

    /** Se ejecuta fuera del hilo principal. */
    fun clasificar(fotos: Map<Modalidad, File>): ResultadoIa

    companion object {
        /**
         * Usa los modelos `modelo/<modalidad>.tflite` que existan en assets; si no hay
         * ninguno, el clasificador simulado de la demo.
         */
        fun crear(context: Context): ClasificadorAnemia {
            val disponibles = context.assets.list("modelo").orEmpty().toSet()
            val modelos = Modalidad.entries
                .filter { "${it.archivo}.tflite" in disponibles }
                .associateWith { ModeloModalidad(context, "modelo/${it.archivo}.tflite") }
            return if (modelos.isEmpty()) ClasificadorSimulado() else ClasificadorTflite(modelos)
        }
    }
}

/**
 * Decodifica la foto reducida (para no cargar en memoria la imagen completa de la cámara)
 * y la endereza según la orientación EXIF que guarda CameraX.
 */
internal fun decodificarReducida(archivo: File, ladoMax: Int): Bitmap? {
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.path, limites)
    var muestreo = 1
    while (maxOf(limites.outWidth, limites.outHeight) / (muestreo * 2) >= ladoMax) muestreo *= 2
    val bitmap = BitmapFactory.decodeFile(archivo.path, BitmapFactory.Options().apply { inSampleSize = muestreo })
        ?: return null
    val grados = when (
        ExifInterface(archivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    ) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> return bitmap
    }
    val matriz = Matrix().apply { postRotate(grados) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
}
