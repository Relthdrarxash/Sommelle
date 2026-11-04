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
 * Mise en forme anglaise : « 11:15 pm », « 6h 15m », « Tuesday, November 3 ».
 * Les unités restent attachées au nombre par une espace insécable, comme en français.
 */
object EnglishFormat : TextFormat {

    private const val NBSP = ' '

    override fun time(time: LocalTime): String {
        val hour = if (time.hour % 12 == 0) 12 else time.hour % 12
        val suffix = if (time.hour < 12) "am" else "pm"
        return "$hour:${time.minute.twoDigits()}${NBSP}$suffix"
    }

    override fun duration(duration: Duration): String {
        val total = duration.toMinutes()
        val hours = total / 60
        val minutes = total % 60
        // Forme courte, comme « 6 h 15 » en français : elle tient dans les tuiles et les graphiques.
        return when {
            hours == 0L -> "$minutes${NBSP}min"
            minutes == 0L -> "${hours}h"
            else -> "${hours}h${NBSP}${minutes.toInt().twoDigits()}m"
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total < 60 -> "$total${NBSP}minute${if (total > 1) "s" else ""}"
            total % 60 == 0L -> "${total / 60}${NBSP}hour${if (total >= 120) "s" else ""}"
            else -> duration(duration.abs())
        }
    }

    override fun percent(ratio: Double): String = "${(ratio * 100).roundToInt()}%"

    override fun dayName(day: DayOfWeek): String = DAYS[day.value - 1]

    /** « Tuesday, November 3 » */
    override fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek)}, ${monthName(date.month)} ${date.dayOfMonth}"

    /** « Night from Monday to Tuesday ». */
    override fun nightLabel(nightOf: LocalDate): String =
        "Night from ${dayName(nightOf.minusDays(1).dayOfWeek)} to ${dayName(nightOf.dayOfWeek)}"

    /** « Tue 3 Nov » : sans ordre jour/mois ambigu. */
    override fun shortNight(nightOf: LocalDate): String =
        "${dayName(nightOf.dayOfWeek).take(3)} ${nightOf.dayOfMonth} ${monthName(nightOf.month).take(3)}"

    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10}.${tenths % 10} / 5"
    }

    override fun observationWeek() = "Observation week"
    override fun week(number: Int) = "Week $number"
    override fun observation() = "Observation"
    override fun inProgress() = " (in progress)"
    override fun baseline() = "At the start"
    override fun noWindow() = "None"
    override fun noAwakening() = "None"
    override fun windowRange(bed: String, wake: String) = "$bed to $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "bedtime $bed, rise time $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "Under 18"
        AgeBand.FROM_18_TO_24 -> "18 to 24"
        AgeBand.FROM_25_TO_44 -> "25 to 44"
        AgeBand.FROM_45_TO_64 -> "45 to 64"
        AgeBand.FROM_65 -> "65 and over"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Full program, with time-in-bed restriction"
        Pathway.WITHOUT_RESTRICTION -> "Program without time-in-bed restriction"
        Pathway.OUT_OF_SCOPE -> "Outside the scope of the program"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "Under 18: referred to a doctor"
        Referral.SLEEP_APNEA_SUSPECTED -> "Reports snoring with breathing pauses: sleep apnea to be investigated"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Reports falling asleep unintentionally during the day"
        Referral.BIPOLAR_DISORDER -> "Reports bipolar disorder: no time-in-bed restriction"
        Referral.EPILEPSY -> "Reports epilepsy: no time-in-bed restriction"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Very late schedule with normal sleep: possible delayed sleep phase"
        Referral.SHIFT_WORK -> "Reports shift or night work"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Activity at risk from drowsiness: minimum window raised to 6 hours"
        Notice.EXTERNAL_DISRUPTION -> "Reports nights interrupted by someone else"
        Notice.PREGNANCY -> "Reports pregnancy: no time-in-bed restriction"
        Notice.CRISIS_RESOURCES -> "Distress expressed in the questionnaire: support resources shown"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Time to fall asleep"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Time awake at night"
        OutcomeMetric.TOTAL_SLEEP -> "Total sleep time"
        OutcomeMetric.EFFICIENCY -> "Sleep efficiency"
        OutcomeMetric.QUALITY -> "Perceived quality"
        OutcomeMetric.DAYTIME_IMPACT -> "Impact on the day, from 0 to 4"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "Improving"
        Trend.STABLE -> "Stable"
        Trend.WORSE -> "Declining"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Week by week. " + bars.joinToString(" ") { "${it.label}: ${it.timeInBed} in bed, ${it.totalSleep} asleep." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else "$this"

    private val DAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    private val MONTHS = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    )
}
