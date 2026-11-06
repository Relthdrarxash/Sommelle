package fr.sommelle.core.presentation

import fr.sommelle.core.model.DiaryEntry
import fr.sommelle.core.model.SleepWindow
import fr.sommelle.core.protocol.PeriodSummary
import fr.sommelle.core.protocol.ProtocolConfig
import fr.sommelle.core.protocol.WindowChange
import fr.sommelle.core.protocol.WindowDecision
import java.time.Duration
import java.time.LocalDate
import java.util.Locale

/** Ton du bilan hebdomadaire. Jamais d'échec : une semaine difficile est une information. */
enum class ReportTone { LONGER, SHORTER, STEADY, AT_FLOOR, AT_MAXIMUM, MISSING_NIGHTS }

/**
 * Contenu du bilan hebdomadaire, déjà mis en forme. L'interface choisit le titre et la phrase
 * selon [tone] et y insère ces valeurs (documentation/design.md, section 8 : une phrase avant un graphique).
 */
data class WeeklyReport(
    val tone: ReportTone,
    /** « 88 % », ou null si la semaine n'a pas assez de nuits. */
    val efficiency: String?,
    /** « 15 minutes », pour les allongements et réductions. */
    val change: String?,
    val bedTime: String,
    val wakeTime: String,
    val loggedNights: Int,
    val requiredNights: Int,
) {
    companion object {
        fun of(
            previous: SleepWindow,
            decision: WindowDecision,
            config: ProtocolConfig = ProtocolConfig.DEFAULT,
            text: TextFormat = Texts.current(),
        ): WeeklyReport {
            val delta: Duration = decision.window.duration.minus(previous.duration)
            val tone = when (decision.change) {
                WindowChange.INCREASED -> ReportTone.LONGER
                WindowChange.DECREASED, WindowChange.DECREASED_TO_FLOOR -> ReportTone.SHORTER
                WindowChange.UNCHANGED_IN_TARGET_RANGE -> ReportTone.STEADY
                WindowChange.UNCHANGED_AT_FLOOR -> ReportTone.AT_FLOOR
                WindowChange.UNCHANGED_AT_MAXIMUM -> ReportTone.AT_MAXIMUM
                WindowChange.UNCHANGED_INSUFFICIENT_DATA -> ReportTone.MISSING_NIGHTS
            }
            val week = decision.week
            return WeeklyReport(
                tone = tone,
                efficiency = week?.takeIf { tone != ReportTone.MISSING_NIGHTS }?.let { text.percent(it.sleepEfficiency) },
                change = if (tone == ReportTone.LONGER || tone == ReportTone.SHORTER) text.durationInWords(delta) else null,
                bedTime = text.time(decision.window.bedTime),
                wakeTime = text.time(decision.window.wakeTime),
                loggedNights = week?.loggedNights ?: 0,
                requiredNights = config.minimumLoggedNightsPerWeek,
            )
        }
    }
}

/** Une ligne du graphique « temps au lit et temps dormi ». */
data class WeekBar(
    val label: String,
    val timeInBed: String,
    val totalSleep: String,
    /** Longueurs relatives, entre 0 et 1, par rapport à la plus longue nuit du graphique. */
    val timeInBedFraction: Float,
    val totalSleepFraction: Float,
)

object WeekChart {
    /** [periods] : la ligne de base puis chaque semaine, dans l'ordre. Les périodes vides sont ignorées. */
    fun bars(periods: List<PeriodSummary?>, text: TextFormat = Texts.current()): List<WeekBar> {
        val labelled = periods.mapIndexedNotNull { index, summary ->
            summary?.let { (if (index == 0) text.baseline() else text.week(index)) to it }
        }
        val max = labelled.maxOfOrNull { it.second.meanTimeInBed.toMinutes() }?.coerceAtLeast(1) ?: return emptyList()
        return labelled.map { (label, s) ->
            WeekBar(
                label = label,
                timeInBed = text.duration(s.meanTimeInBed),
                totalSleep = text.duration(s.meanTotalSleepTime),
                timeInBedFraction = s.meanTimeInBed.toMinutes().toFloat() / max,
                totalSleepFraction = s.meanTotalSleepTime.toMinutes().toFloat() / max,
            )
        }
    }

    /** Description complète lue par TalkBack à la place du graphique. */
    fun accessibilityDescription(bars: List<WeekBar>, text: TextFormat = Texts.current()): String =
        text.chartDescription(bars)
}

/** Représentation d'une nuit récente pour le bandeau d'accès rapide. */
data class RecentNightUi(
    val date: LocalDate,
    val dayLabel: String,
    val dayNumber: String,
    val isFilled: Boolean,
    val isMissingToFill: Boolean,
    val sleepEfficiency: Int? = null,
)

fun recentNights(
    today: LocalDate,
    entries: List<DiaryEntry>,
    nightsToFill: List<LocalDate>,
    locale: Locale = Locale.getDefault(),
): List<RecentNightUi> {
    val entriesByDate = entries.associateBy { it.nightOf }
    return (6 downTo 0).map { offset ->
        val date = today.minusDays(offset.toLong())
        val entry = entriesByDate[date]
        val isFilled = entry != null
        val isMissing = date in nightsToFill
        val label = date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, locale)
        val efficiency = entry?.let {
            val tib = Duration.between(it.bedTime, it.outOfBedTime).toMinutes()
            val sleepPeriod = Duration.between(it.lightsOutTime, it.finalWakeTime)
            val totalSleep = sleepPeriod.minus(it.sleepOnsetLatency).minus(it.wakeAfterSleepOnset).coerceAtLeast(Duration.ZERO).toMinutes()
            if (tib > 0) ((totalSleep.toDouble() / tib) * 100).toInt().coerceIn(0, 100) else null
        }
        RecentNightUi(
            date = date,
            dayLabel = label,
            dayNumber = date.dayOfMonth.toString(),
            isFilled = isFilled,
            isMissingToFill = isMissing,
            sleepEfficiency = efficiency,
        )
    }
}
