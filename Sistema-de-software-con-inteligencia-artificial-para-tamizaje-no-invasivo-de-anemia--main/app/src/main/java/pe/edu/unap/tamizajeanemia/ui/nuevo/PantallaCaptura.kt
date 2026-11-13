package pe.edu.unap.tamizajeanemia.ui.nuevo

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import pe.edu.unap.tamizajeanemia.camara.VistaCamara
import pe.edu.unap.tamizajeanemia.ia.Modalidad
import pe.edu.unap.tamizajeanemia.ia.decodificarReducida

/** Captura guiada de las tres fotos (conjuntiva, palma, uña) y lanzamiento del análisis. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaCaptura(
    vm: NuevoTamizajeViewModel,
    onVolver: () -> Unit,
    onGuardado: (Long) -> Unit,
) {
    val context = LocalContext.current
    var permiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        permiso = it
    }
    LaunchedEffect(Unit) { if (!permiso) pedirPermiso.launch(Manifest.permission.CAMERA) }

    // Paso actual: la primera modalidad sin foto; si ya están todas, se muestra el resumen.
    var revisando by remember { mutableStateOf<Modalidad?>(null) }
    val pendiente = Modalidad.entries.firstOrNull { it !in vm.fotos }
    val paso = revisando ?: pendiente

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(paso?.titulo ?: "Revisar fotos")
                        Text("Paso 2 de 2 · Fotos", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver, enabled = !vm.analizando) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { relleno ->
        Column(Modifier.fillMaxSize().padding(relleno)) {
            Pasos(actual = paso, hechas = vm.fotos.keys, onElegir = { if (!vm.analizando) revisando = it })
            when {
                !permiso -> SinPermiso { pedirPermiso.launch(Manifest.permission.CAMERA) }
                paso != null -> {
                    Text(
                        paso.instruccion,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    VistaCamara(
                        crearArchivo = { vm.nuevoArchivo(paso) },
                        onFoto = { archivo ->
                            vm.guardarFoto(paso, archivo)
                            revisando = null
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        guia = paso.guia,
                    )
                }
                else -> Resumen(vm, onRepetir = { revisando = it }, onAnalizar = { vm.analizarYGuardar(onGuardado) })
            }
        }
    }
}

/** Indicador de pasos: número o check por modalidad; tocar uno permite repetir esa foto. */
@Composable
private fun Pasos(actual: Modalidad?, hechas: Set<Modalidad>, onElegir: (Modalidad) -> Unit) {
    val esquema = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Modalidad.entries.forEachIndexed { i, modalidad ->
            if (i > 0) HorizontalDivider(Modifier.weight(1f).padding(horizontal = 6.dp))
            val hecha = modalidad in hechas
            val esActual = modalidad == actual
            Row(
                Modifier.clip(CircleShape).clickable(enabled = hecha) { onElegir(modalidad) }.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                esActual -> esquema.primary
                                hecha -> esquema.secondary
                                else -> esquema.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hecha && !esActual) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = esquema.onSecondary, modifier = Modifier.size(16.dp))
                    } else {
                        Text(
                            "${i + 1}",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (esActual) esquema.onPrimary else esquema.onSurfaceVariant,
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    modalidad.corto,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (esActual) esquema.primary else esquema.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SinPermiso(onPedir: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "La app necesita la cámara para tomar las fotos del tamizaje. Las fotos se guardan solo en este equipo.",
            textAlign = TextAlign.Center,
        )
        Button(onClick = onPedir, modifier = Modifier.padding(top = 16.dp)) { Text("Permitir cámara") }
    }
}

@Composable
private fun Resumen(
    vm: NuevoTamizajeViewModel,
    onRepetir: (Modalidad) -> Unit,
    onAnalizar: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Revise que cada foto esté nítida, con buena luz y con la zona dentro del recuadro.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Modalidad.entries.forEach { modalidad ->
                val archivo = vm.fotos[modalidad]
                val miniatura = remember(archivo) { archivo?.let { decodificarReducida(it, 256) } }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        miniatura?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = modalidad.titulo,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(84.dp).clip(MaterialTheme.shapes.small),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(modalidad.titulo, style = MaterialTheme.typography.titleSmall)
                            }
                        }
                        OutlinedButton(onClick = { onRepetir(modalidad) }, enabled = !vm.analizando) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Repetir")
                        }
                    }
                }
            }
            vm.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        Button(
            onClick = onAnalizar,
            enabled = !vm.analizando,
            modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
        ) {
            if (vm.analizando) {
                CircularProgressIndicator(Modifier.size(22.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Analizando sin conexión…")
            } else {
                Text("Analizar y guardar")
            }
        }
    }
}
