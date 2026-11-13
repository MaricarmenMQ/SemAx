package pe.edu.unap.tamizajeanemia.clinico

import kotlin.math.max

enum class Sexo(val etiqueta: String) { FEMENINO("Femenino"), MASCULINO("Masculino") }

enum class GradoAnemia(val etiqueta: String) {
    SIN_ANEMIA("Sin anemia"),
    LEVE("Anemia leve"),
    MODERADA("Anemia moderada"),
    SEVERA("Anemia severa"),
}

/**
 * Interpretación de hemoglobina para niños de 6 a 59 meses.
 *
 * PENDIENTE DE VALIDAR por el equipo contra la NTS N° 213-MINSA/DGIESP-2024 y la guía
 * OMS 2024 antes de usarse con datos reales. La corrección por altitud es justamente una
 * variable de estudio del proyecto, por eso está aislada aquí.
 */
object Hemoglobina {
    const val EDAD_MIN_MESES = 6
    const val EDAD_MAX_MESES = 59

    /** Altitud de la ciudad de Puno, usada como valor sugerido en el formulario. */
    const val ALTITUD_PUNO_MSNM = 3825

    /**
     * Ajuste por altitud en g/dL (fórmula CDC, 1989), que se resta a la Hb observada:
     * ajuste = -0.032·A + 0.022·A², con A = altitud en miles de pies (m × 0.0033).
     */
    fun ajustePorAltitud(altitudMsnm: Int): Double {
        if (altitudMsnm < 1000) return 0.0
        val a = altitudMsnm * 0.0033
        return max(0.0, -0.032 * a + 0.022 * a * a)
    }

    fun corregir(hbObservada: Double, altitudMsnm: Int): Double =
        hbObservada - ajustePorAltitud(altitudMsnm)

    /** Puntos de corte OMS 2024 por edad (g/dL), aplicados a la Hb ya corregida. */
    fun clasificar(hbCorregida: Double, edadMeses: Int): GradoAnemia {
        require(edadMeses in EDAD_MIN_MESES..EDAD_MAX_MESES) { "Edad fuera de 6–59 meses" }
        val (corteAnemia, corteModerada) = if (edadMeses < 24) 10.5 to 9.5 else 11.0 to 10.0
        return when {
            hbCorregida >= corteAnemia -> GradoAnemia.SIN_ANEMIA
            hbCorregida >= corteModerada -> GradoAnemia.LEVE
            hbCorregida >= 7.0 -> GradoAnemia.MODERADA
            else -> GradoAnemia.SEVERA
        }
    }
}
