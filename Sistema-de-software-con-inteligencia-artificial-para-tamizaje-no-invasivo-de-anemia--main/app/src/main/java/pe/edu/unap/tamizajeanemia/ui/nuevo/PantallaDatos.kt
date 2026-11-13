package pe.edu.unap.tamizajeanemia.ui.nuevo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import pe.edu.unap.tamizajeanemia.clinico.Hemoglobina
import pe.edu.unap.tamizajeanemia.clinico.Sexo
import pe.edu.unap.tamizajeanemia.ui.Seccion
import pe.edu.unap.tamizajeanemia.ui.edadLegible
import pe.edu.unap.tamizajeanemia.ui.formatearHb

/**
 * Atajos de altitud (m s. n. m.) para llenar rápido el campo. Agregar aquí las localidades del
 * trabajo de campo con su altitud verificada.
 */
private val ALTITUDES = listOf("Puno" to Hemoglobina.ALTITUD_PUNO_MSNM, "Juliaca" to 3824)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDatos(
    vm: NuevoTamizajeViewModel,
    onVolver: () -> Unit,
    onContinuar: () -> Unit,
) {
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nuevo tamizaje")
                        Text("Paso 1 de 2 · Datos del niño", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
                }
                Button(
                    onClick = {
                        error = vm.validarDatos()
                        if (error == null) onContinuar()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("Continuar a las fotos")
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null,
                        modifier = Modifier.padding(start = 8.dp).size(18.dp))
                }
            }
        },
    ) { relleno ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(relleno)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Seccion("Participante", Icons.Filled.Person) {
                OutlinedTextField(
                    value = vm.codigo,
                    onValueChange = { vm.codigo = it.uppercase() },
                    label = { Text("Código del participante") },
                    supportingText = { Text("Sin nombres ni DNI. Ej.: PUNO-001") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = vm.edadMeses,
                    onValueChange = { vm.edadMeses = it.filter(Char::isDigit).take(2) },
                    label = { Text("Edad (meses)") },
                    supportingText = {
                        val edad = vm.edadMeses.toIntOrNull()
                        Text(
                            if (edad != null && edad in Hemoglobina.EDAD_MIN_MESES..Hemoglobina.EDAD_MAX_MESES) {
                                edadLegible(edad)
                            } else {
                                "De 6 a 59 meses"
                            },
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Sexo", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Sexo.entries.forEach { opcion ->
                        val elegido = vm.sexo == opcion
                        FilterChip(
                            selected = elegido,
                            onClick = { vm.sexo = opcion },
                            label = { Text(opcion.etiqueta) },
                            leadingIcon = if (elegido) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            } else {
                                null
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }

            Seccion("Lugar de residencia", Icons.Filled.LocationOn) {
                OutlinedTextField(
                    value = vm.altitud,
                    onValueChange = { vm.altitud = it.filter(Char::isDigit).take(4) },
                    label = { Text("Altitud (m s. n. m.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ALTITUDES.forEach { (lugar, metros) ->
                        FilterChip(
                            selected = vm.altitud == metros.toString(),
                            onClick = { vm.altitud = metros.toString() },
                            label = { Text(lugar, style = MaterialTheme.typography.labelMedium) },
                        )
                    }
                }
            }

            Seccion("Hemoglobina (opcional)", Icons.Filled.Favorite) {
                OutlinedTextField(
                    value = vm.hb,
                    onValueChange = { vm.hb = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5) },
                    label = { Text("Hb medida (g/dL)") },
                    supportingText = { Text("Valor del hemoglobinómetro, sin corregir") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                VistaPreviaHb(vm)
            }
        }
    }
}

/** Muestra en vivo la Hb corregida por altitud y el grado, para que el promotor lo vea al momento. */
@Composable
private fun VistaPreviaHb(vm: NuevoTamizajeViewModel) {
    val hb = vm.hb.replace(',', '.').toDoubleOrNull()?.takeIf { it in 3.0..25.0 } ?: return
    val altitud = vm.altitud.toIntOrNull() ?: return
    val edad = vm.edadMeses.toIntOrNull()?.takeIf { it in Hemoglobina.EDAD_MIN_MESES..Hemoglobina.EDAD_MAX_MESES }
    val corregida = Hemoglobina.corregir(hb, altitud)
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(
                "Corregida por altitud: ${formatearHb(corregida)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                edad?.let { Hemoglobina.clasificar(corregida, it).etiqueta } ?: "Ingrese la edad para ver el grado",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
