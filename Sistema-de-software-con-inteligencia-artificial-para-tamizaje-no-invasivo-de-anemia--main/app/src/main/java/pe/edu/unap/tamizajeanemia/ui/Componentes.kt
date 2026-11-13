package pe.edu.unap.tamizajeanemia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import pe.edu.unap.tamizajeanemia.ui.tema.LocalColoresRiesgo

/**
 * Semáforo de la estimación por imágenes. El corte de 0,5 es provisional y se definirá con la
 * validación de los modelos en el altiplano.
 */
enum class NivelRiesgo(val etiqueta: String, val recomendacion: String) {
    BAJO(
        "Riesgo bajo",
        "Sin señales de palidez en las imágenes. Continuar con los controles de crecimiento y desarrollo.",
    ),
    MEDIO(
        "Riesgo intermedio",
        "Hay algunas señales de palidez. Conviene medir la hemoglobina en el próximo control.",
    ),
    ALTO(
        "Riesgo alto",
        "Se sugiere medir la hemoglobina en el establecimiento de salud lo antes posible.",
    ),
    ;

    companion object {
        fun de(probabilidad: Float) = when {
            probabilidad >= 0.5f -> ALTO
            probabilidad >= 0.3f -> MEDIO
            else -> BAJO
        }
    }
}

/** (color de texto/trazo, color de fondo) del nivel según el tema. */
@Composable
@ReadOnlyComposable
fun NivelRiesgo.colores(): Pair<Color, Color> {
    val c = LocalColoresRiesgo.current
    return when (this) {
        NivelRiesgo.BAJO -> c.bajo to c.bajoFondo
        NivelRiesgo.MEDIO -> c.medio to c.medioFondo
        NivelRiesgo.ALTO -> c.alto to c.altoFondo
    }
}

@Composable
fun InsigniaRiesgo(nivel: NivelRiesgo, modifier: Modifier = Modifier) {
    val (texto, fondo) = nivel.colores()
    Row(
        modifier
            .clip(CircleShape)
            .background(fondo)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(texto))
        Spacer(Modifier.width(6.dp))
        Text(nivel.etiqueta, color = texto, style = MaterialTheme.typography.labelMedium)
    }
}

/** Arco de 0 a 100 % con el valor al centro, coloreado según el nivel de riesgo. */
@Composable
fun IndicadorCircular(
    valor: Float,
    color: Color,
    modifier: Modifier = Modifier,
    tamano: Dp = 160.dp,
    centro: @Composable () -> Unit,
) {
    val pista = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier.size(tamano), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(tamano)) {
            val grosor = size.minDimension * 0.09f
            val arco = Size(size.width - grosor, size.height - grosor)
            val origen = Offset(grosor / 2, grosor / 2)
            drawArc(pista, 135f, 270f, false, origen, arco, style = Stroke(grosor, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * valor.coerceIn(0f, 1f), false, origen, arco,
                style = Stroke(grosor, cap = StrokeCap.Round))
        }
        centro()
    }
}

/** Tarjeta con título e ícono para agrupar campos o datos. */
@Composable
fun Seccion(
    titulo: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    contenido: @Composable () -> Unit,
) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(titulo, style = MaterialTheme.typography.titleMedium)
            }
            contenido()
        }
    }
}

/** Círculo con las iniciales del código del participante. */
@Composable
fun Avatar(codigo: String, color: Color, fondo: Color, modifier: Modifier = Modifier) {
    val iniciales = codigo.filter { it.isLetterOrDigit() }.take(2).uppercase().ifEmpty { "?" }
    Box(
        modifier.size(44.dp).clip(CircleShape).background(fondo),
        contentAlignment = Alignment.Center,
    ) {
        Text(iniciales, color = color, style = MaterialTheme.typography.titleSmall)
    }
}

fun edadLegible(meses: Int): String {
    val anios = meses / 12
    val resto = meses % 12
    return when {
        anios == 0 -> "$meses meses"
        resto == 0 -> if (anios == 1) "1 año" else "$anios años"
        else -> "${if (anios == 1) "1 año" else "$anios años"} y $resto ${if (resto == 1) "mes" else "meses"}"
    }
}
