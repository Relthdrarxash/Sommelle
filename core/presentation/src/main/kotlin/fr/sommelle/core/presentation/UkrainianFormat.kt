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
 * Формат українською для часу, тривалості та дат.
 */
object UkrainianFormat : TextFormat {

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
            hours == 0L -> "$minutes${NBSP}хв"
            minutes == 0L -> "${hours}${NBSP}год"
            else -> {
                val minStr = if (minutes < 10) "0$minutes" else "$minutes"
                "${hours}${NBSP}год${NBSP}${minStr}"
            }
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total == 1L -> "1${NBSP}хвилина"
            total in 2L..4L -> "$total${NBSP}хвилини"
            total < 60L -> "$total${NBSP}хвилин"
            total == 60L -> "1${NBSP}година"
            total in listOf(120L, 180L, 240L) -> "${total / 60}${NBSP}години"
            total % 60L == 0L -> "${total / 60}${NBSP}годин"
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
        return "Ніч з ${DAYS_GENITIVE[prevDay.value - 1]} на ${DAYS_ACCUSATIVE[currDay.value - 1]}"
    }

    override fun shortNight(nightOf: LocalDate): String =
        "${DAYS_SHORT[nightOf.dayOfWeek.value - 1]}., ${nightOf.dayOfMonth}.${nightOf.monthValue}."

    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} / 5"
    }

    override fun observationWeek() = "Тиждень спостереження"
    override fun week(number: Int) = "Тиждень $number"
    override fun observation() = "Спостереження"
    override fun inProgress() = " (триває)"
    override fun baseline() = "На початку"
    override fun noWindow() = "Немає"
    override fun noAwakening() = "Немає"
    override fun windowRange(bed: String, wake: String) = "з $bed до $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "сон $bed, підйом $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "До 18 років"
        AgeBand.FROM_18_TO_24 -> "Від 18 до 24 років"
        AgeBand.FROM_25_TO_44 -> "Від 25 до 44 років"
        AgeBand.FROM_45_TO_64 -> "Від 45 до 64 років"
        AgeBand.FROM_65 -> "65 років і старше"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Повна програма з обмеженням часу в ліжку"
        Pathway.WITHOUT_RESTRICTION -> "Програма без обмеження часу в ліжку"
        Pathway.OUT_OF_SCOPE -> "Поза межами програми"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "До 18 років: рекомендована консультація лікаря"
        Referral.SLEEP_APNEA_SUSPECTED -> "Хропіння із зупинками дихання: перевірте апное сну"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Зголошено про мимовільне засинання вдень"
        Referral.BIPOLAR_DISORDER -> "Зголошено біполярний розлад: без обмеження часу в ліжку"
        Referral.EPILEPSY -> "Зголошено епілепсію: без обмеження часу в ліжку"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Дуже пізній час при нормальному сні: можливий розлад фази сну"
        Referral.SHIFT_WORK -> "Зголошено змінну або нічну роботу"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Діяльність із ризиком сонливості: мінімальне вікно піднято до 6 год"
        Notice.EXTERNAL_DISRUPTION -> "Зголошено ночі, перервані іншими людьми"
        Notice.PREGNANCY -> "Зголошено вагітність: без обмеження часу в ліжку"
        Notice.CRISIS_RESOURCES -> "Зголошено пригнічений стан: доступна психологічна підтримка"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Час засинання"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Час без сну вночі"
        OutcomeMetric.TOTAL_SLEEP -> "Загальний час сну"
        OutcomeMetric.EFFICIENCY -> "Ефективність сну"
        OutcomeMetric.QUALITY -> "Суб'єктивна якість"
        OutcomeMetric.DAYTIME_IMPACT -> "Вплив на день (від 0 до 4)"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "Покращення"
        Trend.STABLE -> "Стабільно"
        Trend.WORSE -> "Зниження"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Динаміка за тижнями. " + bars.joinToString(" ") { "${it.label}: ${it.timeInBed} у ліжку, ${it.totalSleep} сну." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private val DAYS = listOf("понеділок", "вівторок", "середа", "четвер", "п'ятниця", "субота", "неділя")
    private val DAYS_GENITIVE = listOf("понеділка", "вівторка", "середи", "четверга", "п'ятниці", "суботи", "неділі")
    private val DAYS_ACCUSATIVE = listOf("понеділок", "вівторок", "середу", "четвер", "п'ятницю", "суботу", "неділю")
    private val DAYS_SHORT = listOf("пн", "вт", "ср", "чт", "пт", "сб", "нд")
    private val MONTHS = listOf(
        "січня", "лютого", "березня", "квітня", "травня", "червня",
        "липня", "серпня", "вересня", "жовтня", "листопада", "грудня",
    )
}
