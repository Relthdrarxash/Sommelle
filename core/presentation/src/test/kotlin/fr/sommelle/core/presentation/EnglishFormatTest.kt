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

class EnglishFormatTest {

    private fun nb(s: String) = s.replace(' ', ' ')

    @Test
    fun `heures en anglais`() {
        assertEquals(nb("11:15 pm"), EnglishFormat.time(LocalTime.of(23, 15)))
        assertEquals(nb("12:30 am"), EnglishFormat.time(LocalTime.of(0, 30)))
        assertEquals(nb("6:05 am"), EnglishFormat.time(LocalTime.of(6, 5)))
        assertEquals(nb("12:00 pm"), EnglishFormat.time(LocalTime.of(12, 0)))
    }

    @Test
    fun `durees en anglais`() {
        assertEquals(nb("45 min"), EnglishFormat.duration(Duration.ofMinutes(45)))
        assertEquals(nb("6h 15m"), EnglishFormat.duration(Duration.ofMinutes(375)))
        assertEquals(nb("7h 05m"), EnglishFormat.duration(Duration.ofMinutes(425)))
        assertEquals("1h", EnglishFormat.duration(Duration.ofMinutes(60)))
        assertEquals(nb("15 minutes"), EnglishFormat.durationInWords(Duration.ofMinutes(-15)))
        assertEquals(nb("1 hour"), EnglishFormat.durationInWords(Duration.ofMinutes(60)))
    }

    @Test
    fun `dates et nombres en anglais`() {
        assertEquals("88%", EnglishFormat.percent(0.8812))
        assertEquals("Tuesday, November 3", EnglishFormat.longDate(LocalDate.of(2026, 11, 3)))
        assertEquals("Night from Monday to Tuesday", EnglishFormat.nightLabel(LocalDate.of(2026, 11, 3)))
        assertEquals("Tue 3 Nov", EnglishFormat.shortNight(LocalDate.of(2026, 11, 3)))
        assertEquals("3.5 / 5", EnglishFormat.quality(3.5))
    }

    @Test
    fun `choix de la langue`() {
        assertSame(FrenchFormat, Texts.forLanguage("fr"))
        assertSame(EnglishFormat, Texts.forLanguage("en"))
        assertSame(EnglishFormat, Texts.forLanguage("de"))
    }

    @Test
    fun `aucun libelle vide ni reste de francais`() {
        val labels = AgeBand.entries.map(EnglishFormat::age) +
            Pathway.entries.map(EnglishFormat::pathway) +
            Referral.entries.map(EnglishFormat::referral) +
            Notice.entries.map(EnglishFormat::notice) +
            OutcomeMetric.entries.map(EnglishFormat::metric) +
            Trend.entries.map(EnglishFormat::trend)
        labels.forEach { label ->
            assertTrue(label.isNotBlank())
            assertTrue(label.none { it in "éèàçù" }, "accent dans « $label »")
        }
    }
}
