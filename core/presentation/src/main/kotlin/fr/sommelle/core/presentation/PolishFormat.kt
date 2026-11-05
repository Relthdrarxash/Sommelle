package fr.sommelle.core.presentation

import fr.sommelle.core.model.AgeBand
import fr.sommelle.core.model.Notice
import fr.sommelle.core.model.Pathway
import fr.sommelle.core.model.Referral
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import kotlin.math.roundToInt

/**
 * Format w języku polskim dla godzin, czasu trwania i dat.
 */
object PolishFormat : TextFormat {

    private const val NBSP = ' '

    override fun time(time: LocalTime): String {
        val minutes = time.minute
        val formattedMin = if (minutes < 10) "0$minutes" else "$minutes"
        return "${time.hour}:${formattedMin}"
    }

    override fun duration(duration: Duration): String {
        val total = duration.toMinutes()
        val hours = total / 60
        val minutes = total % 60
        return when {
            hours == 0L -> "$minutes${NBSP}min"
            minutes == 0L -> "${hours}${NBSP}godz."
            else -> {
                val minStr = if (minutes < 10) "0$minutes" else "$minutes"
                "${hours}${NBSP}godz.${NBSP}${minStr}"
            }
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total == 1L -> "1${NBSP}minuta"
            total in 2L..4L -> "$total${NBSP}minuty"
            total < 60L -> "$total${NBSP}minut"
            total == 60L -> "1${NBSP}godzina"
            total in listOf(120L, 180L, 240L) -> "${total / 60}${NBSP}godziny"
            total % 60L == 0L -> "${total / 60}${NBSP}godzin"
            else -> duration(duration.abs())
        }
    }

    override fun percent(ratio: Double): String = "${(ratio * 100).roundToInt()}%"

    override fun dayName(day: DayOfWeek): String = DAYS[day.value - 1]

    override fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek).replaceFirstChar { it.uppercase() }}, ${date.dayOfMonth} ${monthName(date.month)}"

    override fun nightLabel(nightOf: LocalDate): String {
        val prevDay = nightOf.minusDays(1).dayOfWeek
        val currDay = nightOf.dayOfWeek
        val prep = if (prevDay == DayOfWeek.WEDNESDAY) "ze" else "z"
        return "Noc $prep ${DAYS_GENITIVE[prevDay.value - 1]} na ${DAYS_ACCUSATIVE[currDay.value - 1]}"
    }

    override fun shortNight(nightOf: LocalDate): String =
        "${DAYS_SHORT[nightOf.dayOfWeek.value - 1]}. ${nightOf.dayOfMonth}.${nightOf.monthValue}."

    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} / 5"
    }

    override fun observationWeek() = "Tydzień obserwacji"
    override fun week(number: Int) = "Tydzień $number"
    override fun observation() = "Obserwacja"
    override fun inProgress() = " (w toku)"
    override fun baseline() = "Na początku"
    override fun noWindow() = "Brak"
    override fun noAwakening() = "Brak"
    override fun windowRange(bed: String, wake: String) = "od $bed do $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "sen $bed, pobudka $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "Poniżej 18 lat"
        AgeBand.FROM_18_TO_24 -> "18 do 24 lat"
        AgeBand.FROM_25_TO_44 -> "25 do 44 lat"
        AgeBand.FROM_45_TO_64 -> "45 do 64 lat"
        AgeBand.FROM_65 -> "65 lat i więcej"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Pełny program z ograniczeniem czasu w łóżku"
        Pathway.WITHOUT_RESTRICTION -> "Program bez ograniczenia czasu w łóżku"
        Pathway.OUT_OF_SCOPE -> "Poza zakresem programu"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "Poniżej 18 lat: zalecana konsultacja lekarska"
        Referral.SLEEP_APNEA_SUSPECTED -> "Chrapanie z bezdechami: skonsultuj możliwe bezdechy senne"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Zgłoszone mimowolne zasypianie w ciągu dnia"
        Referral.BIPOLAR_DISORDER -> "Zgłoszona choroba afektywna dwubiegunowa: bez ograniczenia czasu w łóżku"
        Referral.EPILEPSY -> "Zgłoszona padaczka: bez ograniczenia czasu w łóżku"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Bardzo późne pory przy normalnym śnie: możliwe opóźnienie fazy snu"
        Referral.SHIFT_WORK -> "Zgłoszona praca zmianowa lub nocna"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Czynność z ryzykiem senności: minimalne okno snu podniesione do 6 godz."
        Notice.EXTERNAL_DISRUPTION -> "Zgłoszone noce przerywane przez osoby trzecie"
        Notice.PREGNANCY -> "Zgłoszona ciąża: bez ograniczenia czasu w łóżku"
        Notice.CRISIS_RESOURCES -> "Zgłoszone pogorszenie nastroju: dostępne wsparcie kryzysowe"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Czas zasypiania"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Czas czuwania w nocy"
        OutcomeMetric.TOTAL_SLEEP -> "Całkowity czas snu"
        OutcomeMetric.EFFICIENCY -> "Efektywność snu"
        OutcomeMetric.QUALITY -> "Odczuwana jakość"
        OutcomeMetric.DAYTIME_IMPACT -> "Wpływ na dzień (od 0 do 4)"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "Poprawa"
        Trend.STABLE -> "Stabilnie"
        Trend.WORSE -> "Spadek"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Przebieg według tygodni. " + bars.joinToString(" ") { "${it.label}: ${it.timeInBed} w łóżku, ${it.totalSleep} snu." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private val DAYS = listOf("poniedziałek", "wtorek", "środa", "czwartek", "piątek", "sobota", "niedziela")
    private val DAYS_GENITIVE = listOf("poniedziałku", "wtorku", "środy", "czwartku", "piątku", "soboty", "niedzieli")
    private val DAYS_ACCUSATIVE = listOf("poniedziałek", "wtorek", "środę", "czwartek", "piątek", "sobotę", "niedzielę")
    private val DAYS_SHORT = listOf("pon", "wt", "śr", "czw", "pt", "sob", "niedz")
    private val MONTHS = listOf(
        "stycznia", "lutego", "marca", "kwietnia", "maja", "czerwca",
        "lipca", "sierpnia", "września", "października", "listopada", "grudnia",
    )
}
