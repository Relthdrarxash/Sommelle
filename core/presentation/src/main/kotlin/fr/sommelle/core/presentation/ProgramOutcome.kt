package fr.sommelle.core.presentation

import fr.sommelle.core.model.DiaryEntry
import fr.sommelle.core.protocol.PeriodSummary
import fr.sommelle.core.protocol.ProtocolConfig
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Ressenti de la journée, demandé au début puis à la fin du programme :
 * « À quel point vos nuits gênent-elles vos journées ? », de 0 (pas du tout) à 4 (énormément).
 * Question propre à Sommelle, non validée : elle complète les mesures de l'agenda, sans les remplacer.
 */
data class DaytimeImpact(val date: LocalDate, val score: Int) {
    init { require(score in SCALE) { "score hors échelle" } }

    companion object {
        val SCALE = 0..4
    }
}

enum class OutcomeMetric {
    SLEEP_ONSET,
    AWAKE_AT_NIGHT,
    TOTAL_SLEEP,
    EFFICIENCY,
    QUALITY,
    DAYTIME_IMPACT,
    ;

    /** Libellé dans la langue de l'appli. */
    val label: String get() = Texts.current().metric(this)
}

/** Sens de l'évolution, du point de vue du sommeil. Jamais présenté comme un échec. */
enum class Trend {
    BETTER,
    STABLE,
    WORSE,
    ;

    /** Libellé dans la langue de l'appli. */
    val word: String get() = Texts.current().trend(this)
}

data class OutcomeRow(val metric: OutcomeMetric, val before: String, val after: String, val trend: Trend)

/**
 * Bilan avant/après : la semaine d'observation comparée aux 7 dernières nuits, à partir des mesures
 * de l'agenda du sommeil (Consensus Sleep Diary), complétées par le ressenti de la journée.
 */
object ProgramOutcome {

    /** Le bilan est proposé à partir de cette semaine du programme. */
    const val AVAILABLE_FROM_WEEK = 6

    /** Les 7 nuits qui précèdent [today] (incluse), si au moins le minimum requis est renseigné. */
    fun recentPeriod(entries: List<DiaryEntry>, today: LocalDate, config: ProtocolConfig = ProtocolConfig.DEFAULT): PeriodSummary? {
        val recent = entries.filter { it.nightOf > today.minusDays(7) && it.nightOf <= today }
        return PeriodSummary.of(recent)?.takeIf { it.loggedNights >= config.minimumLoggedNightsPerWeek }
    }

    fun compare(
        baseline: PeriodSummary?,
        recent: PeriodSummary?,
        impactBefore: DaytimeImpact?,
        impactAfter: DaytimeImpact?,
        text: TextFormat = Texts.current(),
    ): List<OutcomeRow> {
        val rows = mutableListOf<OutcomeRow>()
        if (baseline != null && recent != null) {
            rows += minutesRow(OutcomeMetric.SLEEP_ONSET, baseline.meanSleepOnsetLatency.toMinutes(), recent.meanSleepOnsetLatency.toMinutes(), lowerIsBetter = true, text)
            rows += minutesRow(OutcomeMetric.AWAKE_AT_NIGHT, baseline.meanWakeAfterSleepOnset.toMinutes(), recent.meanWakeAfterSleepOnset.toMinutes(), lowerIsBetter = true, text)
            rows += minutesRow(OutcomeMetric.TOTAL_SLEEP, baseline.meanTotalSleepTime.toMinutes(), recent.meanTotalSleepTime.toMinutes(), lowerIsBetter = false, text)
            rows += OutcomeRow(
                OutcomeMetric.EFFICIENCY,
                text.percent(baseline.sleepEfficiency),
                text.percent(recent.sleepEfficiency),
                trend(recent.sleepEfficiency - baseline.sleepEfficiency, threshold = 0.02, lowerIsBetter = false),
            )
            rows += OutcomeRow(
                OutcomeMetric.QUALITY,
                text.quality(baseline.meanQualityScore),
                text.quality(recent.meanQualityScore),
                trend(recent.meanQualityScore - baseline.meanQualityScore, threshold = 0.3, lowerIsBetter = false),
            )
        }
        if (impactBefore != null && impactAfter != null && impactAfter.date > impactBefore.date) {
            rows += OutcomeRow(
                OutcomeMetric.DAYTIME_IMPACT,
                "${impactBefore.score} / 4",
                "${impactAfter.score} / 4",
                trend((impactAfter.score - impactBefore.score).toDouble(), threshold = 0.5, lowerIsBetter = true),
            )
        }
        return rows
    }

    private fun minutesRow(metric: OutcomeMetric, before: Long, after: Long, lowerIsBetter: Boolean, text: TextFormat) = OutcomeRow(
        metric,
        text.duration(java.time.Duration.ofMinutes(before)),
        text.duration(java.time.Duration.ofMinutes(after)),
        trend((after - before).toDouble(), threshold = 5.0, lowerIsBetter = lowerIsBetter),
    )

    private fun trend(delta: Double, threshold: Double, lowerIsBetter: Boolean): Trend = when {
        abs(delta) < threshold -> Trend.STABLE
        (delta < 0) == lowerIsBetter -> Trend.BETTER
        else -> Trend.WORSE
    }

    /** Moyenne de la qualité ressentie, sur 5, avec une décimale. */
    fun qualityText(score: Double, text: TextFormat = Texts.current()): String = text.quality(score)
}
