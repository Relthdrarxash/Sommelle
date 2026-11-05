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

class PolishFormatTest {

    private fun nb(s: String) = s.replace(' ', ' ')

    @Test
    fun `godziny po polsku`() {
        assertEquals("23:15", PolishFormat.time(LocalTime.of(23, 15)))
        assertEquals("0:30", PolishFormat.time(LocalTime.of(0, 30)))
        assertEquals("6:05", PolishFormat.time(LocalTime.of(6, 5)))
        assertEquals("12:00", PolishFormat.time(LocalTime.of(12, 0)))
    }

    @Test
    fun `czasy trwania po polsku`() {
        assertEquals(nb("45 min"), PolishFormat.duration(Duration.ofMinutes(45)))
        assertEquals(nb("6 godz. 15"), PolishFormat.duration(Duration.ofMinutes(375)))
        assertEquals(nb("7 godz. 05"), PolishFormat.duration(Duration.ofMinutes(425)))
        assertEquals(nb("1 godz."), PolishFormat.duration(Duration.ofMinutes(60)))
        assertEquals(nb("15 minut"), PolishFormat.durationInWords(Duration.ofMinutes(-15)))
        assertEquals(nb("1 minuta"), PolishFormat.durationInWords(Duration.ofMinutes(1)))
        assertEquals(nb("2 minuty"), PolishFormat.durationInWords(Duration.ofMinutes(2)))
        assertEquals(nb("1 godzina"), PolishFormat.durationInWords(Duration.ofMinutes(60)))
        assertEquals(nb("2 godziny"), PolishFormat.durationInWords(Duration.ofMinutes(120)))
        assertEquals(nb("5 godzin"), PolishFormat.durationInWords(Duration.ofMinutes(300)))
    }

    @Test
    fun `daty i liczby po polsku`() {
        assertEquals("88%", PolishFormat.percent(0.8812))
        assertEquals("Wtorek, 3 listopada", PolishFormat.longDate(LocalDate.of(2026, 11, 3)))
        assertEquals("Noc z poniedziałku na wtorek", PolishFormat.nightLabel(LocalDate.of(2026, 11, 3)))
        assertEquals("wt. 3.11.", PolishFormat.shortNight(LocalDate.of(2026, 11, 3)))
        assertEquals("3,5 / 5", PolishFormat.quality(3.5))
    }

    @Test
    fun `wybor formatu`() {
        assertSame(PolishFormat, Texts.forLanguage("pl"))
    }

    @Test
    fun `brak pustych etykiet`() {
        val labels = AgeBand.entries.map(PolishFormat::age) +
            Pathway.entries.map(PolishFormat::pathway) +
            Referral.entries.map(PolishFormat::referral) +
            Notice.entries.map(PolishFormat::notice) +
            OutcomeMetric.entries.map(PolishFormat::metric) +
            Trend.entries.map(PolishFormat::trend)
        labels.forEach { label ->
            assertTrue(label.isNotBlank())
        }
    }
}
