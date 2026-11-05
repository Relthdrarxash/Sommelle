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

class GermanFormatTest {

    private fun nb(s: String) = s.replace(' ', ' ')

    @Test
    fun `uhrzeiten auf deutsch`() {
        assertEquals(nb("23:15 Uhr"), GermanFormat.time(LocalTime.of(23, 15)))
        assertEquals(nb("0:30 Uhr"), GermanFormat.time(LocalTime.of(0, 30)))
        assertEquals(nb("6:05 Uhr"), GermanFormat.time(LocalTime.of(6, 5)))
        assertEquals(nb("12:00 Uhr"), GermanFormat.time(LocalTime.of(12, 0)))
    }

    @Test
    fun `dauern auf deutsch`() {
        assertEquals(nb("45 Min."), GermanFormat.duration(Duration.ofMinutes(45)))
        assertEquals(nb("6h 15"), GermanFormat.duration(Duration.ofMinutes(375)))
        assertEquals(nb("7h 05"), GermanFormat.duration(Duration.ofMinutes(425)))
        assertEquals("1h", GermanFormat.duration(Duration.ofMinutes(60)))
        assertEquals(nb("15 Minuten"), GermanFormat.durationInWords(Duration.ofMinutes(-15)))
        assertEquals(nb("1 Stunde"), GermanFormat.durationInWords(Duration.ofMinutes(60)))
        assertEquals(nb("2 Stunden"), GermanFormat.durationInWords(Duration.ofMinutes(120)))
    }

    @Test
    fun `daten und zahlen auf deutsch`() {
        assertEquals("88%", GermanFormat.percent(0.8812))
        assertEquals("Dienstag, 3. November", GermanFormat.longDate(LocalDate.of(2026, 11, 3)))
        assertEquals("Nacht von Montag auf Dienstag", GermanFormat.nightLabel(LocalDate.of(2026, 11, 3)))
        assertEquals("Di., 3.11.", GermanFormat.shortNight(LocalDate.of(2026, 11, 3)))
        assertEquals("3,5 / 5", GermanFormat.quality(3.5))
    }

    @Test
    fun `auswahl des formats`() {
        assertSame(GermanFormat, Texts.forLanguage("de"))
    }

    @Test
    fun `keine leeren bezeichnungen`() {
        val labels = AgeBand.entries.map(GermanFormat::age) +
            Pathway.entries.map(GermanFormat::pathway) +
            Referral.entries.map(GermanFormat::referral) +
            Notice.entries.map(GermanFormat::notice) +
            OutcomeMetric.entries.map(GermanFormat::metric) +
            Trend.entries.map(GermanFormat::trend)
        labels.forEach { label ->
            assertTrue(label.isNotBlank())
        }
    }
}
