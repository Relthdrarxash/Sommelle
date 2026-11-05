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

class UkrainianFormatTest {

    private fun nb(s: String) = s.replace(' ', ' ')

    @Test
    fun `години українською`() {
        assertEquals("23:15", UkrainianFormat.time(LocalTime.of(23, 15)))
        assertEquals("0:30", UkrainianFormat.time(LocalTime.of(0, 30)))
        assertEquals("6:05", UkrainianFormat.time(LocalTime.of(6, 5)))
        assertEquals("12:00", UkrainianFormat.time(LocalTime.of(12, 0)))
    }

    @Test
    fun `тривалість українською`() {
        assertEquals(nb("45 хв"), UkrainianFormat.duration(Duration.ofMinutes(45)))
        assertEquals(nb("6 год 15"), UkrainianFormat.duration(Duration.ofMinutes(375)))
        assertEquals(nb("7 год 05"), UkrainianFormat.duration(Duration.ofMinutes(425)))
        assertEquals(nb("1 год"), UkrainianFormat.duration(Duration.ofMinutes(60)))
        assertEquals(nb("15 хвилин"), UkrainianFormat.durationInWords(Duration.ofMinutes(-15)))
        assertEquals(nb("1 хвилина"), UkrainianFormat.durationInWords(Duration.ofMinutes(1)))
        assertEquals(nb("2 хвилини"), UkrainianFormat.durationInWords(Duration.ofMinutes(2)))
        assertEquals(nb("1 година"), UkrainianFormat.durationInWords(Duration.ofMinutes(60)))
        assertEquals(nb("2 години"), UkrainianFormat.durationInWords(Duration.ofMinutes(120)))
        assertEquals(nb("5 годин"), UkrainianFormat.durationInWords(Duration.ofMinutes(300)))
    }

    @Test
    fun `дати та числа українською`() {
        assertEquals("88%", UkrainianFormat.percent(0.8812))
        assertEquals("Вівторок, 3 листопада", UkrainianFormat.longDate(LocalDate.of(2026, 11, 3)))
        assertEquals("Ніч з понеділка на вівторок", UkrainianFormat.nightLabel(LocalDate.of(2026, 11, 3)))
        assertEquals("вт., 3.11.", UkrainianFormat.shortNight(LocalDate.of(2026, 11, 3)))
        assertEquals("3,5 / 5", UkrainianFormat.quality(3.5))
    }

    @Test
    fun `вибір формату`() {
        assertSame(UkrainianFormat, Texts.forLanguage("uk"))
    }

    @Test
    fun `немає порожніх міток`() {
        val labels = AgeBand.entries.map(UkrainianFormat::age) +
            Pathway.entries.map(UkrainianFormat::pathway) +
            Referral.entries.map(UkrainianFormat::referral) +
            Notice.entries.map(UkrainianFormat::notice) +
            OutcomeMetric.entries.map(UkrainianFormat::metric) +
            Trend.entries.map(UkrainianFormat::trend)
        labels.forEach { label ->
            assertTrue(label.isNotBlank())
        }
    }
}
