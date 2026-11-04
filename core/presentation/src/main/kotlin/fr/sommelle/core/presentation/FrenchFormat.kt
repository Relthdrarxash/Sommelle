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
 * Mise en forme française des heures, durées et dates, telle qu'affichée et lue par TalkBack.
 * Les heures s'écrivent « 23 h 15 » et non « 23:15 » : plus lisible, et correctement prononcé
 * par les synthèses vocales.
 */
object FrenchFormat : TextFormat {

    /** Espace insécable, pour qu'un nombre ne soit jamais séparé de son unité. */
    const val NBSP = ' '

    override fun time(time: LocalTime): String {
        val minutes = time.minute
        return if (minutes == 0) "${time.hour}${NBSP}h" else "${time.hour}${NBSP}h${NBSP}${minutes.twoDigits()}"
    }

    override fun duration(duration: Duration): String {
        val total = duration.toMinutes()
        val hours = total / 60
        val minutes = total % 60
        return when {
            hours == 0L -> "$minutes${NBSP}min"
            minutes == 0L -> "$hours${NBSP}h"
            else -> "$hours${NBSP}h${NBSP}${minutes.toInt().twoDigits()}"
        }
    }

    override fun durationInWords(duration: Duration): String {
        val total = duration.abs().toMinutes()
        return when {
            total < 60 -> "$total${NBSP}minute${if (total > 1) "s" else ""}"
            total % 60 == 0L -> "${total / 60}${NBSP}heure${if (total >= 120) "s" else ""}"
            else -> duration(duration.abs())
        }
    }

    override fun percent(ratio: Double): String = "${(ratio * 100).roundToInt()}${NBSP}%"

    override fun dayName(day: DayOfWeek): String = DAYS[day.value - 1]

    /** « Mardi 3 novembre » */
    override fun longDate(date: LocalDate): String =
        "${dayName(date.dayOfWeek).replaceFirstChar { it.uppercase() }} ${date.dayOfMonth} ${monthName(date.month)}"

    /** « Nuit de lundi à mardi », pour la nuit qui se termine le matin de [nightOf]. */
    override fun nightLabel(nightOf: LocalDate): String =
        "Nuit de ${dayName(nightOf.minusDays(1).dayOfWeek)} à ${dayName(nightOf.dayOfWeek)}"

    override fun shortNight(nightOf: LocalDate): String =
        "${dayName(nightOf.dayOfWeek).take(3)}. ${nightOf.dayOfMonth}/${nightOf.monthValue}"

    /** Une décimale à la française : « 3,5 / 5 ». */
    override fun quality(score: Double): String {
        val tenths = (score * 10).roundToInt()
        return "${tenths / 10},${tenths % 10} / 5"
    }

    override fun observationWeek() = "Semaine d'observation"
    override fun week(number: Int) = "Semaine $number"
    override fun observation() = "Observation"
    override fun inProgress() = " (en cours)"
    override fun baseline() = "Au départ"
    override fun noWindow() = "Aucune"
    override fun noAwakening() = "Aucun"
    override fun windowRange(bed: String, wake: String) = "$bed à $wake"
    override fun currentWindow(bed: String, wake: String, duration: String) = "coucher $bed, lever $wake ($duration)"
    override fun awakenings(count: Int, total: String) = "$count × $total"

    override fun age(age: AgeBand) = when (age) {
        AgeBand.UNDER_18 -> "Moins de 18 ans"
        AgeBand.FROM_18_TO_24 -> "18 à 24 ans"
        AgeBand.FROM_25_TO_44 -> "25 à 44 ans"
        AgeBand.FROM_45_TO_64 -> "45 à 64 ans"
        AgeBand.FROM_65 -> "65 ans et plus"
    }

    override fun pathway(pathway: Pathway) = when (pathway) {
        Pathway.FULL -> "Programme complet, avec restriction du temps au lit"
        Pathway.WITHOUT_RESTRICTION -> "Programme sans restriction du temps au lit"
        Pathway.OUT_OF_SCOPE -> "Hors du périmètre du programme"
    }

    override fun referral(referral: Referral) = when (referral) {
        Referral.MINOR -> "Moins de 18 ans : orientation vers un médecin"
        Referral.SLEEP_APNEA_SUSPECTED -> "Ronflements avec pauses respiratoires déclarés : apnée du sommeil à explorer"
        Referral.EXCESSIVE_DAYTIME_SLEEPINESS -> "Endormissements involontaires en journée déclarés"
        Referral.BIPOLAR_DISORDER -> "Trouble bipolaire déclaré : pas de restriction du temps au lit"
        Referral.EPILEPSY -> "Épilepsie déclarée : pas de restriction du temps au lit"
        Referral.DELAYED_SLEEP_PHASE_SUSPECTED -> "Horaires très tardifs avec sommeil normal : retard de phase possible"
        Referral.SHIFT_WORK -> "Travail posté ou de nuit déclaré"
    }

    override fun notice(notice: Notice) = when (notice) {
        Notice.REINFORCED_DROWSINESS_WARNING -> "Activité à risque de somnolence : plancher de fenêtre relevé à 6 h"
        Notice.EXTERNAL_DISRUPTION -> "Nuits interrompues par un tiers déclarées"
        Notice.PREGNANCY -> "Grossesse déclarée : pas de restriction du temps au lit"
        Notice.CRISIS_RESOURCES -> "Détresse exprimée au questionnaire : ressources d'aide affichées"
    }

    override fun metric(metric: OutcomeMetric) = when (metric) {
        OutcomeMetric.SLEEP_ONSET -> "Temps d'endormissement"
        OutcomeMetric.AWAKE_AT_NIGHT -> "Éveils nocturnes"
        OutcomeMetric.TOTAL_SLEEP -> "Temps de sommeil"
        OutcomeMetric.EFFICIENCY -> "Efficacité du sommeil"
        OutcomeMetric.QUALITY -> "Qualité ressentie"
        OutcomeMetric.DAYTIME_IMPACT -> "Gêne dans la journée, de 0 à 4"
    }

    override fun trend(trend: Trend) = when (trend) {
        Trend.BETTER -> "En amélioration"
        Trend.STABLE -> "Stable"
        Trend.WORSE -> "En recul"
    }

    override fun chartDescription(bars: List<WeekBar>): String =
        "Évolution par semaine. " + bars.joinToString(" ") { "${it.label} : ${it.timeInBed} au lit, ${it.totalSleep} de sommeil." }

    private fun monthName(month: Month): String = MONTHS[month.value - 1]

    private fun Int.twoDigits(): String = if (this < 10) "0$this" else "$this"

    private val DAYS = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche")
    private val MONTHS = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )
}
