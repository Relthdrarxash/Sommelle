package fr.sommelle.core.presentation

import fr.sommelle.core.model.AgeBand
import fr.sommelle.core.model.Notice
import fr.sommelle.core.model.Pathway
import fr.sommelle.core.model.Referral
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SpanishFormatTest {

    private fun nb(s: String) = s.replace(' ', ' ')

    @Test
    fun `horas en espanol`() {
        assertEquals("23:15", SpanishFormat.time(LocalTime.of(23, 15)))
        assertEquals("0:30", SpanishFormat.time(LocalTime.of(0, 30)))
        assertEquals("6:05", SpanishFormat.time(LocalTime.of(6, 5)))
        assertEquals("12:00", SpanishFormat.time(LocalTime.of(12, 0)))
    }

    @Test
    fun `duraciones en espanol`() {
        assertEquals(nb("45 min"), SpanishFormat.duration(Duration.ofMinutes(45)))
        assertEquals(nb("6h 15"), SpanishFormat.duration(Duration.ofMinutes(375)))
        assertEquals(nb("7h 05"), SpanishFormat.duration(Duration.ofMinutes(425)))
        assertEquals("1h", SpanishFormat.duration(Duration.ofMinutes(60)))
        assertEquals(nb("15 minutos"), SpanishFormat.durationInWords(Duration.ofMinutes(-15)))
        assertEquals(nb("1 hora"), SpanishFormat.durationInWords(Duration.ofMinutes(60)))
        assertEquals(nb("2 horas"), SpanishFormat.durationInWords(Duration.ofMinutes(120)))
    }

    @Test
    fun `fechas y numeros en espanol`() {
        assertEquals("88%", SpanishFormat.percent(0.8812))
        assertEquals("Martes, 3 de noviembre", SpanishFormat.longDate(LocalDate.of(2026, 11, 3)))
        assertEquals("Noche del lunes al martes", SpanishFormat.nightLabel(LocalDate.of(2026, 11, 3)))
        assertEquals("mar. 3/11", SpanishFormat.shortNight(LocalDate.of(2026, 11, 3)))
        assertEquals("3,5 / 5", SpanishFormat.quality(3.5))
    }

    @Test
    fun `seleccion del formato`() {
        assertSame(SpanishFormat, Texts.forLanguage("es"))
    }

    @Test
    fun `ningun texto vacio`() {
        val labels = AgeBand.entries.map(SpanishFormat::age) +
            Pathway.entries.map(SpanishFormat::pathway) +
            Referral.entries.map(SpanishFormat::referral) +
            Notice.entries.map(SpanishFormat::notice) +
            OutcomeMetric.entries.map(SpanishFormat::metric) +
            Trend.entries.map(SpanishFormat::trend)
        labels.forEach { label ->
            assertTrue(label.isNotBlank())
        }
    }
}
