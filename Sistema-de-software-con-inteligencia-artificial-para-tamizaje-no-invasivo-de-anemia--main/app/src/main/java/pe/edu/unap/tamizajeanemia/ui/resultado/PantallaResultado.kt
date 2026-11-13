package pe.edu.unap.tamizajeanemia.ui.resultado

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import pe.edu.unap.tamizajeanemia.clinico.GradoAnemia
import pe.edu.unap.tamizajeanemia.clinico.Hemoglobina
import pe.edu.unap.tamizajeanemia.datos.Tamizaje
import pe.edu.unap.tamizajeanemia.ia.Modalidad
import pe.edu.unap.tamizajeanemia.ia.decodificarReducida
import pe.edu.unap.tamizajeanemia.ui.IndicadorCircular
import pe.edu.unap.tamizajeanemia.ui.InsigniaRiesgo
import pe.edu.unap.tamizajeanemia.ui.NivelRiesgo
import pe.edu.unap.tamizajeanemia.ui.Seccion
import pe.edu.unap.tamizajeanemia.ui.colores
import pe.edu.unap.tamizajeanemia.ui.edadLegible
import pe.edu.unap.tamizajeanemia.ui.formatearFecha
import pe.edu.unap.tamizajeanemia.ui.formatearHb
import pe.edu.unap.tamizajeanemia.ui.formatearPorcentaje
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaResultado(
    tamizaje: Flow<Tamizaje?>,
    onVolver: () -> Unit,
    onNuevo: () -> Unit,
) {
    val t by tamizaje.collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resultado") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { relleno ->
        val actual = t ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Estimacion(actual)
            PorZona(actual)
            HemoglobinaReferencia(actual)
            Seccion("Participante", Icons.Filled.Person) {
                Dato("Código", actual.codigoParticipante)
                Dato("Edad", "${edadLegible(actual.edadMeses)} (${actual.edadMeses} meses)")
                Dato("Sexo", actual.sexo.etiqueta)
                Dato("Altitud", "${actual.altitudMsnm} m s. n. m.")
                Dato("Fecha", formatearFecha(actual.fechaMillis))
            }
            Aviso(actual.iaSimulada)
            Button(onClick = onNuevo, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nuevo tamizaje")
            }
            OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Volver al inicio")
            }
        }
    }
}

@Composable
private fun Estimacion(t: Tamizaje) {
    val nivel = NivelRiesgo.de(t.probabilidadIa)
    val (color, fondo) = nivel.colores()
    Card(
        colors = CardDefaults.cardColors(containerColor = fondo.copy(alpha = 0.55f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Estimación por imágenes", style = MaterialTheme.typography.titleMedium)
            IndicadorCircular(valor = t.probabilidadIa, color = color) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(formatearPorcentaje(t.probabilidadIa), style = MaterialTheme.typography.displaySmall, color = color)
                    Text("probabilidad\nde anemia", style = MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            InsigniaRiesgo(nivel)
            Text(nivel.recomendacion, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun PorZona(t: Tamizaje) {
    val zonas = listOf(
        Triple(Modalidad.CONJUNTIVA, t.probConjuntiva, t.rutaConjuntiva),
        Triple(Modalidad.PALMA, t.probPalma, t.rutaPalma),
        Triple(Modalidad.UNA, t.probUna, t.rutaUna),
    )
    Seccion("Resultado por zona", Icons.Filled.Search) {
        zonas.forEachIndexed { i, (modalidad, prob, ruta) ->
            if (i > 0) HorizontalDivider()
            val miniatura = remember(ruta) { decodificarReducida(File(ruta), 160) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                miniatura?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = modalidad.titulo,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(MaterialTheme.shapes.small),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(modalidad.corto, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        Text(prob?.let { formatearPorcentaje(it) } ?: "Sin modelo",
                            style = MaterialTheme.typography.titleSmall,
                            color = prob?.let { NivelRiesgo.de(it).colores().first }
                                ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (prob != null) {
                        LinearProgressIndicator(
                            progress = { prob },
                            color = NivelRiesgo.de(prob).colores().first,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier.fillMaxWidth().height(8.dp),
                        )
                    } else {
                        Text("La foto se guardó para el dataset, pero aún no se analiza.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Text("Modelo: ${t.modeloIa}", style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HemoglobinaReferencia(t: Tamizaje) {
    Seccion("Hemoglobina de referencia", Icons.Filled.Favorite) {
        val hb = t.hbObservada
        val corregida = t.hbCorregida
        if (hb == null || corregida == null) {
            Text("No se registró medición con hemoglobinómetro.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Seccion
        }
        val grado = Hemoglobina.clasificar(corregida, t.edadMeses)
        val nivel = when (grado) {
            GradoAnemia.SIN_ANEMIA -> NivelRiesgo.BAJO
            GradoAnemia.LEVE -> NivelRiesgo.MEDIO
            else -> NivelRiesgo.ALTO
        }
        Dato("Observada", formatearHb(hb))
        Dato("Ajuste por altitud", "−" + formatearHb(Hemoglobina.ajustePorAltitud(t.altitudMsnm)))
        Dato("Corregida", formatearHb(corregida))
        Text(grado.etiqueta, style = MaterialTheme.typography.titleMedium, color = nivel.colores().first)
        Text(
            "A más altura el cuerpo produce más hemoglobina, por eso se resta un ajuste antes de comparar con los puntos de corte.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Aviso(simulado: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
        Row(Modifier.padding(12.dp)) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                if (simulado) {
                    "DEMO: aún no hay un modelo entrenado. Este resultado es simulado y no tiene valor clínico."
                } else {
                    "Modelos provisionales en investigación, entrenados con fotos de niños de Ghana y aún no " +
                        "validados en el altiplano. Herramienta de apoyo: no reemplaza el diagnóstico clínico " +
                        "ni la medición oficial de hemoglobina."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}
