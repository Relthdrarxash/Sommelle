package fr.sommelle.core.presentation

import fr.sommelle.core.model.DiaryEntry
import fr.sommelle.core.model.ScreeningResult
import fr.sommelle.core.model.SleepQuality
import fr.sommelle.core.protocol.NightMetrics
import fr.sommelle.core.protocol.PeriodSummary
import fr.sommelle.core.protocol.ProgramPlanner
import fr.sommelle.core.protocol.ProtocolConfig
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/** Une ligne du tableau hebdomadaire du résumé. */
data class ReportWeek(
    val label: String,
    val nights: Int,
    val timeInBed: String,
    val totalSleep: String,
    val efficiency: String,
    val sleepOnset: String,
    val awake: String,
    val quality: String,
    /** Fenêtre de sommeil prescrite cette semaine-là, ou « Aucune ». */
    val window: String,
)

/** Une nuit du détail de l'agenda. */
data class ReportNight(
    val night: String,
    val bedTime: String,
    val outOfBed: String,
    val sleepOnset: String,
    val awake: String,
    val totalSleep: String,
    val efficiency: String,
    val quality: String,
)

/**
 * Résumé destiné à un professionnel de santé, entièrement mis en forme ici pour que le PDF,
 * l'écran et les tests partagent le même contenu. Le résumé ne contient ni nom ni identifiant :
 * la personne l'écrit à la main si elle le souhaite.
 */
data class ClinicianReport(
    val generatedOn: String,
    val programStart: String,
    val programWeek: String,
    val ageBand: String,
    val pathway: String,
    val safetyNotes: List<String>,
    val currentWindow: String?,
    val weeks: List<ReportWeek>,
    val outcome: List<OutcomeRow>,
    val nights: List<ReportNight>,
) {
    companion object {
        /** Nombre de nuits détaillées : les quatre dernières semaines. */
        const val DETAILED_NIGHTS = 28

        fun build(
            today: LocalDate,
            startDate: LocalDate,
            screening: ScreeningResult,
            entries: List<DiaryEntry>,
            windows: List<StoredWindow>,
            impacts: List<DaytimeImpact>,
            config: ProtocolConfig = ProtocolConfig.DEFAULT,
            text: TextFormat = Texts.current(),
        ): ClinicianReport {
            val sortedWindows = windows.sortedBy { it.window.effectiveFrom }
            val week = ProgramPlanner(config = config).programWeek(startDate, today)
            val periods = weekPeriods(today, startDate, sortedWindows, config)
            val baseline = PeriodSummary.of(nightsBetween(entries, periods.first().first, periods.first().second))
            val recent = ProgramOutcome.recentPeriod(entries, today, config)
            val sortedImpacts = impacts.sortedBy { it.date }
            val showOutcome = (week ?: 0) >= ProgramOutcome.AVAILABLE_FROM_WEEK

            return ClinicianReport(
                generatedOn = text.longDate(today),
                programStart = text.longDate(startDate),
                programWeek = when (week) {
                    null, 0 -> text.observationWeek()
                    else -> text.week(week)
                },
                ageBand = text.age(screening.ageBand),
                pathway = text.pathway(screening.pathway),
                safetyNotes = screening.referrals.map(text::referral) + screening.notices.map(text::notice),
                currentWindow = sortedWindows.lastOrNull()?.window?.let {
                    text.currentWindow(text.time(it.bedTime), text.time(it.wakeTime), text.duration(it.duration))
                },
                weeks = periods.mapIndexedNotNull { index, (from, to) ->
                    val summary = PeriodSummary.of(nightsBetween(entries, from, to)) ?: return@mapIndexedNotNull null
                    val window = sortedWindows.lastOrNull { it.window.effectiveFrom <= from }?.window
                    val label = if (index == 0) text.observation() else text.week(index) + if (to >= today) text.inProgress() else ""
                    ReportWeek(
                        label = label,
                        nights = summary.loggedNights,
                        timeInBed = text.duration(summary.meanTimeInBed),
                        totalSleep = text.duration(summary.meanTotalSleepTime),
                        efficiency = text.percent(summary.sleepEfficiency),
                        sleepOnset = text.duration(summary.meanSleepOnsetLatency),
                        awake = text.duration(summary.meanWakeAfterSleepOnset),
                        quality = text.quality(summary.meanQualityScore),
                        window = window?.let { text.windowRange(text.time(it.bedTime), text.time(it.wakeTime)) } ?: text.noWindow(),
                    )
                },
                outcome = if (showOutcome) {
                    ProgramOutcome.compare(baseline, recent, sortedImpacts.firstOrNull(), sortedImpacts.lastOrNull(), text)
                } else {
                    emptyList()
                },
                nights = entries.sortedByDescending { it.nightOf }.take(DETAILED_NIGHTS).map { night(it, text) },
            )
        }

        /** Périodes : la ligne de base, puis une semaine par fenêtre, jusqu'à aujourd'hui. */
        private fun weekPeriods(
            today: LocalDate,
            startDate: LocalDate,
            windows: List<StoredWindow>,
            config: ProtocolConfig,
        ): List<Pair<LocalDate, LocalDate>> {
            val starts = windows.map { it.window.effectiveFrom }
            val baselineEnd = starts.firstOrNull()?.minusDays(1) ?: startDate.plusDays(config.baselineDays - 1L)
            val weeks = starts.mapIndexed { i, from ->
                val to = starts.getOrNull(i + 1)?.minusDays(1) ?: minOf(from.plusDays(6), today)
                from to to
            }
            return listOf(startDate to baselineEnd) + weeks
        }

        private fun nightsBetween(entries: List<DiaryEntry>, from: LocalDate, to: LocalDate) =
            entries.filter { it.nightOf >= from && it.nightOf <= to }

        private fun night(entry: DiaryEntry, text: TextFormat): ReportNight {
            val m = NightMetrics.of(entry)
            return ReportNight(
                night = text.shortNight(entry.nightOf),
                bedTime = text.time(entry.bedTime.toLocalTime()),
                outOfBed = text.time(entry.outOfBedTime.toLocalTime()),
                sleepOnset = text.duration(entry.sleepOnsetLatency),
                awake = if (entry.awakeningCount == 0) text.noAwakening() else text.awakenings(entry.awakeningCount, text.duration(entry.wakeAfterSleepOnset)),
                totalSleep = text.duration(m.totalSleepTime),
                efficiency = text.percent(m.sleepEfficiency),
                quality = "${entry.quality.score} / 5",
            )
        }

    }
}

/**
 * Agenda complet au format CSV, lisible par un tableur français (séparateur point-virgule,
 * UTF-8 avec marque d'ordre des octets pour Excel).
 */
object DiaryCsv {
    private const val BOM = "﻿"

    val HEADER = listOf(
        "nuit", "coucher", "extinction", "endormissement_min", "reveils", "eveils_min",
        "reveil_final", "lever", "qualite_sur_5", "temps_au_lit_min", "sommeil_min", "efficacite_pct",
    )

    fun export(entries: List<DiaryEntry>): String = buildString {
        append(BOM)
        append(HEADER.joinToString(";")).append("\r\n")
        entries.sortedBy { it.nightOf }.forEach { e ->
            val m = NightMetrics.of(e)
            append(
                listOf(
                    e.nightOf.toString(),
                    e.bedTime.toString(),
                    e.lightsOutTime.toString(),
                    e.sleepOnsetLatency.toMinutes().toString(),
                    e.awakeningCount.toString(),
                    e.wakeAfterSleepOnset.toMinutes().toString(),
                    e.finalWakeTime.toString(),
                    e.outOfBedTime.toString(),
                    e.quality.score.toString(),
                    m.timeInBed.toMinutes().toString(),
                    m.totalSleepTime.toMinutes().toString(),
                    Math.round(m.sleepEfficiency * 100).toString(),
                ).joinToString(";"),
            ).append("\r\n")
        }
    }

    /**
     * Restaure des nuits depuis un fichier CSV exporté par Sommelle (séparateur `;` ou `,`).
     * Les lignes d'en-tête ou mal formées sont ignorées sans erreur.
     */
    fun parse(csv: String): List<DiaryEntry> =
        csv.removePrefix(BOM)
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("nuit") }
            .mapNotNull { line ->
                val delimiter = if (';' in line) ';' else ','
                val cols = line.split(delimiter)
                if (cols.size < 9) return@mapNotNull null
                runCatching {
                    val nightOf = LocalDate.parse(cols[0].trim())
                    val bedTime = LocalDateTime.parse(cols[1].trim())
                    val lightsOutTime = LocalDateTime.parse(cols[2].trim())
                    val sleepOnsetLatency = Duration.ofMinutes(cols[3].trim().toLong().coerceAtLeast(0))
                    val awakeningCount = cols[4].trim().toInt().coerceAtLeast(0)
                    val wakeAfterSleepOnset = Duration.ofMinutes(cols[5].trim().toLong().coerceAtLeast(0))
                    val finalWakeTime = LocalDateTime.parse(cols[6].trim())
                    val outOfBedTime = LocalDateTime.parse(cols[7].trim())
                    val qualityScore = cols[8].trim().toInt().coerceIn(1, 5)
                    val quality = SleepQuality.entries.first { it.score == qualityScore }
                    DiaryEntry(
                        nightOf = nightOf,
                        bedTime = bedTime,
                        lightsOutTime = lightsOutTime,
                        sleepOnsetLatency = sleepOnsetLatency,
                        awakeningCount = awakeningCount,
                        wakeAfterSleepOnset = wakeAfterSleepOnset,
                        finalWakeTime = finalWakeTime,
                        outOfBedTime = outOfBedTime,
                        quality = quality,
                    )
                }.getOrNull()
            }
            .toList()
}
