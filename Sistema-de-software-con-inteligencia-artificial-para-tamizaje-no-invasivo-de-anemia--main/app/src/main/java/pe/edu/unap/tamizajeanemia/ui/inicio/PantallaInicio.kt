package pe.edu.unap.tamizajeanemia.ui.inicio

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pe.edu.unap.tamizajeanemia.datos.Tamizaje
import pe.edu.unap.tamizajeanemia.ui.Avatar
import pe.edu.unap.tamizajeanemia.ui.InsigniaRiesgo
import pe.edu.unap.tamizajeanemia.ui.NivelRiesgo
import pe.edu.unap.tamizajeanemia.ui.colores
import pe.edu.unap.tamizajeanemia.ui.edadLegible
import pe.edu.unap.tamizajeanemia.ui.formatearFecha
import pe.edu.unap.tamizajeanemia.ui.formatearPorcentaje
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaInicio(
    tamizajes: Flow<List<Tamizaje>>,
    estadoIa: String,
    onNuevo: () -> Unit,
    onAbrir: (Long) -> Unit,
    exportar: suspend (OutputStream) -> Int,
) {
    val lista by tamizajes.collectAsStateWithLifecycle(initialValue = null)
    val context = LocalContext.current
    val alcance = rememberCoroutineScope()
    val elegirDestino = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        alcance.launch {
            val mensaje = try {
                val total = context.contentResolver.openOutputStream(uri)!!.use { exportar(it) }
                "Se exportaron $total tamizajes"
            } catch (e: Exception) {
                "No se pudo exportar: ${e.message}"
            }
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tamizaje de anemia") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = {
                    if (!lista.isNullOrEmpty()) {
                        IconButton(onClick = {
                            val fecha = SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(Date())
                            elegirDestino.launch("tamizajes_$fecha.zip")
                        }) { Icon(Icons.Filled.Share, contentDescription = "Exportar tamizajes") }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo tamizaje") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { relleno ->
        val items = lista ?: return@Scaffold
        LazyColumn(
            Modifier.fillMaxSize().padding(relleno),
            contentPadding = PaddingValues(16.dp, 4.dp, 16.dp, 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Encabezado(items, estadoIa) }
            if (items.isEmpty()) {
                item { EstadoVacio() }
            } else {
                item {
                    Text(
                        "Tamizajes recientes",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                    )
                }
                items(items, key = { it.id }) { t -> TarjetaTamizaje(t) { onAbrir(t.id) } }
            }
        }
    }
}

@Composable
private fun Encabezado(tamizajes: List<Tamizaje>, estadoIa: String) {
    val esquema = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(Brush.linearGradient(listOf(esquema.primary, esquema.primary.copy(alpha = 0.78f), esquema.secondary)))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column {
            Text("Semillero AXIS EPIS · UNAP", style = MaterialTheme.typography.labelLarge, color = esquema.onPrimary.copy(alpha = 0.85f))
            Text(
                "Tamizaje no invasivo de anemia infantil",
                style = MaterialTheme.typography.titleLarge,
                color = esquema.onPrimary,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Dato("Tamizajes", tamizajes.size.toString(), Modifier.weight(1f))
            Dato("Riesgo alto", tamizajes.count { NivelRiesgo.de(it.probabilidadIa) == NivelRiesgo.ALTO }.toString(), Modifier.weight(1f))
            Dato("Con Hb", tamizajes.count { it.hbObservada != null }.toString(), Modifier.weight(1f))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = esquema.onPrimary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(estadoIa, style = MaterialTheme.typography.bodySmall, color = esquema.onPrimary)
        }
    }
}

@Composable
private fun Dato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.16f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(valor, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
        Text(etiqueta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f))
    }
}

@Composable
private fun TarjetaTamizaje(t: Tamizaje, onClick: () -> Unit) {
    val nivel = NivelRiesgo.de(t.probabilidadIa)
    val (color, fondo) = nivel.colores()
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(t.codigoParticipante, color, fondo)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.codigoParticipante, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${edadLegible(t.edadMeses)} · ${t.sexo.etiqueta}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    formatearFecha(t.fechaMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatearPorcentaje(t.probabilidadIa), style = MaterialTheme.typography.titleLarge, color = color)
                Spacer(Modifier.height(4.dp))
                InsigniaRiesgo(nivel)
                if (t.iaSimulada) {
                    Text("simulado", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun EstadoVacio() {
    val pasos = listOf(
        "Registre la edad, el sexo y la altitud del niño (con un código, sin nombres).",
        "Tome las fotos guiadas de conjuntiva, palma y uña.",
        "Revise la estimación y, si hace falta, derive a medición de hemoglobina.",
    )
    Column(
        Modifier.fillMaxWidth().padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Aún no hay tamizajes", style = MaterialTheme.typography.titleLarge)
        pasos.forEachIndexed { i, paso ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${i + 1}",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(top = 5.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.width(12.dp))
                Text(paso, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
