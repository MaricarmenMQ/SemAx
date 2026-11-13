package pe.edu.unap.tamizajeanemia.ui.tema

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Granate (sangre / hemoglobina) como primario y turquesa andino como secundario.
private val Claro = lightColorScheme(
    primary = Color(0xFF9C2C3D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9DC),
    onPrimaryContainer = Color(0xFF400011),
    secondary = Color(0xFF1F6B6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFBCEBEE),
    onSecondaryContainer = Color(0xFF002022),
    tertiary = Color(0xFF7A5900),
    tertiaryContainer = Color(0xFFFFDF9E),
    onTertiaryContainer = Color(0xFF261A00),
    background = Color(0xFFFFF8F7),
    surface = Color(0xFFFFF8F7),
    surfaceVariant = Color(0xFFF4DDDE),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF0F0),
    surfaceContainer = Color(0xFFFCEAEA),
    surfaceContainerHigh = Color(0xFFF6E4E4),
    outlineVariant = Color(0xFFD8C1C2),
)

private val Oscuro = darkColorScheme(
    primary = Color(0xFFFFB2B9),
    onPrimary = Color(0xFF660A22),
    primaryContainer = Color(0xFF7E1328),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Color(0xFFA0CFD2),
    onSecondary = Color(0xFF003739),
    secondaryContainer = Color(0xFF004F52),
    onSecondaryContainer = Color(0xFFBCEBEE),
    tertiary = Color(0xFFF2C14E),
    tertiaryContainer = Color(0xFF5C4300),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = Color(0xFF1A1112),
    surface = Color(0xFF1A1112),
    surfaceContainerLowest = Color(0xFF140C0D),
    surfaceContainerLow = Color(0xFF22191A),
    surfaceContainer = Color(0xFF271D1E),
    surfaceContainerHigh = Color(0xFF322728),
)

/** Colores del semáforo de riesgo, con versión para fondo y para texto. */
@Immutable
data class ColoresRiesgo(
    val bajo: Color, val bajoFondo: Color,
    val medio: Color, val medioFondo: Color,
    val alto: Color, val altoFondo: Color,
)

private val RiesgoClaro = ColoresRiesgo(
    bajo = Color(0xFF1E7A3C), bajoFondo = Color(0xFFD5F2DC),
    medio = Color(0xFF946200), medioFondo = Color(0xFFFFE8B3),
    alto = Color(0xFFB3261E), altoFondo = Color(0xFFFFDAD6),
)

private val RiesgoOscuro = ColoresRiesgo(
    bajo = Color(0xFF8BD8A0), bajoFondo = Color(0xFF0F3D1E),
    medio = Color(0xFFF2C14E), medioFondo = Color(0xFF4A3400),
    alto = Color(0xFFFFB4AB), altoFondo = Color(0xFF690005),
)

val LocalColoresRiesgo = staticCompositionLocalOf { RiesgoClaro }

private val Tipografia = Typography().run {
    copy(
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
        displaySmall = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp),
    )
}

private val Formas = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
)

@Composable
fun TemaTamizaje(contenido: @Composable () -> Unit) {
    val oscuro = isSystemInDarkTheme()
    CompositionLocalProvider(
        LocalColoresRiesgo provides if (oscuro) RiesgoOscuro else RiesgoClaro,
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) Oscuro else Claro,
            typography = Tipografia,
            shapes = Formas,
            content = contenido,
        )
    }
}
