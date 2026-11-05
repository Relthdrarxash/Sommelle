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
 * Deutsches Format für Uhrzeiten, Dauern und Daten.
 */
object GermanFormat : TextFormat {

    private const val NBSP = ' '

    override fun time(time: LocalTime): String {
        val minutes = time.minute
        val formattedMin = if (minutes < 10) "0$minutes" else "$minutes"
        return "${time.hour}:${formattedMin}${NBSP}Uhr"
    }

    override fun duration(duration: Duration): String {
        val total = duration.toMinutes()
        val hours = total / 60
        val minutes = total % 60
        return when {
            hours == 0L -> "$minutes${NBSP}Min."
            minutes == 0L -> "${hours}h"
            else -> {
                val minStr = if (minutes < 10) "0$minutes" else "$minutes"
                "${hours}h${NBSP}${minStr}"
            }
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total < 60 -> "$total${NBSP}Minute${if (total > 1) "n" else ""}"
            total % 60 == 0L -> "${total / 60}${NBSP}Stunde${if (total >= 120) "n" else ""}"
            else -> duration(duration.abs())
        }
    }

    override fun percent(ratio: Double): String = "${(ratio * 100).roundToInt()}%"

    override fun dayName(day: DayOfWeek): String = DAYS[day.value - 1]

    override fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek)}, ${date.dayOfMonth}. ${monthName(date.month)}"

    override fun nightLabel(nightOf: LocalDate): String =
        "Nacht von ${dayName(nightOf.minusDays(1).dayOfWeek)} auf ${dayName(nightOf.dayOfWeek)}"

    override fun shortNight(nightOf: LocalDate): String =
        "${dayName(nightOf.dayOfWeek).take(2)}., ${nightOf.dayOfMonth}.${nightOf.monthValue}."

    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} / 5"
    }

    override fun observationWeek() = "Beobachtungswoche"
    override fun week(number: Int) = "Woche $number"
    override fun observation() = "Beobachtung"
    override fun inProgress() = " (laufend)"
    override fun baseline() = "Zu Beginn"
    override fun noWindow() = "Keines"
    override fun noAwakening() = "Keines"
    override fun windowRange(bed: String, wake: String) = "$bed bis $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "Schlafenszeit $bed, Aufstehen $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "Unter 18 Jahre"
        AgeBand.FROM_18_TO_24 -> "18 bis 24 Jahre"
        AgeBand.FROM_25_TO_44 -> "25 bis 44 Jahre"
        AgeBand.FROM_45_TO_64 -> "45 bis 64 Jahre"
        AgeBand.FROM_65 -> "65 Jahre und älter"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Vollständiges Programm mit Bettzeit-Einschränkung"
        Pathway.WITHOUT_RESTRICTION -> "Programm ohne Bettzeit-Einschränkung"
        Pathway.OUT_OF_SCOPE -> "Außerhalb des Anwendungsbereichs"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "Unter 18 Jahre: Ärztliche Konsultation empfohlen"
        Referral.SLEEP_APNEA_SUSPECTED -> "Schnarchen mit Atemaussetzern: Schlafapnoe abklären"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Ungewolltes Einschlafen am Tag angegeben"
        Referral.BIPOLAR_DISORDER -> "Bipolare Störung angegeben: Keine Bettzeit-Einschränkung"
        Referral.EPILEPSY -> "Epilepsie angegeben: Keine Bettzeit-Einschränkung"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Sehr späte Zeiten bei normalem Schlaf: Mögliche Phasenverzögerung"
        Referral.SHIFT_WORK -> "Schicht- oder Nachtarbeit angegeben"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Sicherheitsrelevante Tätigkeit: Mindestfenster auf 6 Std. angehoben"
        Notice.EXTERNAL_DISRUPTION -> "Nächtliche Störungen durch Dritte angegeben"
        Notice.PREGNANCY -> "Schwangerschaft angegeben: Keine Bettzeit-Einschränkung"
        Notice.CRISIS_RESOURCES -> "Seelische Belastung geäußert: Hilfsangebote verfügbar"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Einschlafdauer"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Wachzeit in der Nacht"
        OutcomeMetric.TOTAL_SLEEP -> "Gesamtschlafdauer"
        OutcomeMetric.EFFICIENCY -> "Schlafeffizienz"
        OutcomeMetric.QUALITY -> "Gefühlte Schlafqualität"
        OutcomeMetric.DAYTIME_IMPACT -> "Tagesbeeinträchtigung (0 bis 4)"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "Verbessert"
        Trend.STABLE -> "Stabil"
        Trend.WORSE -> "Verschlechtert"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Verlauf nach Wochen. " + bars.joinToString(" ") { "${it.label}: ${it.timeInBed} im Bett, ${it.totalSleep} geschlafen." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private val DAYS = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag")
    private val MONTHS = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember",
    )
}
