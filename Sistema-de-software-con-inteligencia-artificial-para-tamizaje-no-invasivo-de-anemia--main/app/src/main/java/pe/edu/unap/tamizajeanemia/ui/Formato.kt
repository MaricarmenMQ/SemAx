package pe.edu.unap.tamizajeanemia.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val localePeru = Locale.forLanguageTag("es-PE")

fun formatearFecha(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", localePeru).format(Date(millis))

fun formatearPorcentaje(valor: Float): String = String.format(localePeru, "%.0f %%", valor * 100)

fun formatearHb(valor: Double): String = String.format(localePeru, "%.1f g/dL", valor)
