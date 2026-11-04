package fr.sommelle.core.presentation

import fr.sommelle.core.model.AgeBand
import fr.sommelle.core.model.Notice
import fr.sommelle.core.model.Pathway
import fr.sommelle.core.model.Referral
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

/**
 * Mise en forme des heures, durées, dates et libellés calculés par ce module, dans une langue donnée.
 * Les textes fixes de l'interface sont dans les ressources Android ; ici, seulement ce qui est composé
 * à partir des données (bilans, résumé pour le médecin, graphique).
 */
interface TextFormat {
    fun time(time: LocalTime): String
    fun duration(duration: Duration): String

    /** Écart de durée, toujours positif : « 15 minutes », « 1 heure ». */
    fun durationInWords(duration: Duration): String
    fun percent(ratio: Double): String
    fun dayName(day: DayOfWeek): String
    fun longDate(date: LocalDate): String

    /** Nuit qui se termine le matin de [nightOf], avec une majuscule initiale. */
    fun nightLabel(nightOf: LocalDate): String

    /** Nuit abrégée pour un tableau : « mar. 3/11 ». */
    fun shortNight(nightOf: LocalDate): String

    /** Moyenne sur 5 avec une décimale. */
    fun quality(score: Double): String

    fun observationWeek(): String
    fun week(number: Int): String
    fun observation(): String
    fun inProgress(): String
    fun baseline(): String
    fun noWindow(): String
    fun noAwakening(): String
    fun windowRange(bed: String, wake: String): String
    fun currentWindow(bed: String, wake: String, duration: String): String
    fun awakenings(count: Int, total: String): String

    fun age(age: AgeBand): String
    fun pathway(pathway: Pathway): String
    fun referral(referral: Referral): String
    fun notice(notice: Notice): String
    fun metric(metric: OutcomeMetric): String
    fun trend(trend: Trend): String

    /** Description du graphique lue par TalkBack. */
    fun chartDescription(bars: List<WeekBar>): String
}

object Texts {
    /** Langue de l'appli : le français pour un téléphone en français, l'anglais sinon. */
    fun current(): TextFormat = forLanguage(Locale.getDefault().language)

    fun forLanguage(language: String): TextFormat = if (language == "fr") FrenchFormat else EnglishFormat
}
