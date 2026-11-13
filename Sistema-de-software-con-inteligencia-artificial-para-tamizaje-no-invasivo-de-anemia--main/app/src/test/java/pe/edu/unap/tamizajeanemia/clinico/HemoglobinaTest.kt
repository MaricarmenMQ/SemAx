package pe.edu.unap.tamizajeanemia.clinico

import org.junit.Assert.assertEquals
import org.junit.Test

class HemoglobinaTest {

    @Test
    fun sinAjusteBajoMilMetros() {
        assertEquals(0.0, Hemoglobina.ajustePorAltitud(150), 0.0)
    }

    @Test
    fun ajusteEnPunoRondaTresGramos() {
        assertEquals(3.1, Hemoglobina.ajustePorAltitud(3825), 0.05)
    }

    @Test
    fun clasificaSegunEdad() {
        // 10.7 g/dL corregida: normal a los 12 meses, anemia leve a los 36 meses.
        assertEquals(GradoAnemia.SIN_ANEMIA, Hemoglobina.clasificar(10.7, 12))
        assertEquals(GradoAnemia.LEVE, Hemoglobina.clasificar(10.7, 36))
        assertEquals(GradoAnemia.MODERADA, Hemoglobina.clasificar(8.0, 36))
        assertEquals(GradoAnemia.SEVERA, Hemoglobina.clasificar(6.5, 36))
    }

    @Test
    fun hbObservadaEnAltitudSeCorrigeAntesDeClasificar() {
        // 13.0 g/dL en Puno equivale a ~9.9 g/dL corregida: anemia moderada a los 36 meses.
        val corregida = Hemoglobina.corregir(13.0, 3825)
        assertEquals(GradoAnemia.MODERADA, Hemoglobina.clasificar(corregida, 36))
    }
}
