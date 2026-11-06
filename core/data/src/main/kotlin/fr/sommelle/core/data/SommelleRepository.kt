package fr.sommelle.core.data

import android.content.Context
import fr.sommelle.core.data.db.SommelleDao
import fr.sommelle.core.data.db.SommelleDatabase
import fr.sommelle.core.model.DiaryEntry
import fr.sommelle.core.model.ScreeningAnswers
import fr.sommelle.core.model.ScreeningResult
import fr.sommelle.core.data.db.DaytimeImpactEntity
import fr.sommelle.core.data.db.ModuleProgressEntity
import fr.sommelle.core.data.db.SettingsEntity
import fr.sommelle.core.presentation.CognitiveModule
import fr.sommelle.core.presentation.DaytimeImpact
import fr.sommelle.core.presentation.DiaryCsv
import fr.sommelle.core.presentation.ProgramEngine
import fr.sommelle.core.presentation.ReminderSettings
import fr.sommelle.core.presentation.StoredWindow
import fr.sommelle.core.presentation.WindowStep
import fr.sommelle.core.protocol.PeriodSummary
import fr.sommelle.core.protocol.ProtocolConfig
import fr.sommelle.core.protocol.RestrictionProfile
import fr.sommelle.core.protocol.ScreeningEvaluator
import fr.sommelle.core.protocol.SleepWindowCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** Implémenté par l'application : donne accès au dépôt là où l'injection n'est pas possible (tâches de fond). */
interface SommelleRepositoryProvider {
    val repository: SommelleRepository
}

/** Le programme en cours, tel qu'enregistré. */
data class Program(
    val startDate: LocalDate,
    val answers: ScreeningAnswers,
    val screening: ScreeningResult,
    val wakeTime: LocalTime?,
)

/** Tout ce que les écrans affichent, lu en une fois. */
data class ProgramSnapshot(
    val program: Program,
    val entries: List<DiaryEntry>,
    val windows: List<StoredWindow>,
)

/** Synthèse de la semaine d'observation pour le sas de transition vers la restriction (Semaine 2). */
data class BaselineTransitionInfo(
    val summary: PeriodSummary,
    val profile: RestrictionProfile?,
    val suggestedWakeTime: LocalTime?,
    val prescribedDuration: Duration,
)

/**
 * Point d'accès unique aux données, toutes stockées sur le téléphone.
 * Aucune méthode n'envoie quoi que ce soit sur le réseau.
 */
class SommelleRepository(
    private val dao: SommelleDao,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val config: ProtocolConfig = ProtocolConfig.DEFAULT,
) {
    private val evaluator = ScreeningEvaluator(config)
    private val engine = ProgramEngine(config)
    private val calculator = SleepWindowCalculator(config)

    fun today(): LocalDate = LocalDate.now(clock)

    fun now(): LocalTime = LocalTime.now(clock)

    /** null tant que le questionnaire d'entrée n'a pas été rempli. */
    val snapshot: Flow<ProgramSnapshot?> = combine(dao.program(), dao.entries(), dao.windows()) { p, e, w ->
        p?.let {
            val answers = it.answers()
            ProgramSnapshot(
                program = Program(it.startDate, answers, evaluator.evaluate(answers), it.wakeTime),
                entries = e.map { entity -> entity.toModel() },
                windows = w.map { entity -> entity.toModel() },
            )
        }
    }

    fun evaluate(answers: ScreeningAnswers): ScreeningResult = evaluator.evaluate(answers)

    /** Démarre le programme aujourd'hui. Les nuits déjà saisies, s'il y en a, sont conservées. */
    suspend fun startProgram(answers: ScreeningAnswers) {
        dao.upsertProgram(answers.toEntity(startDate = today(), wakeTime = null))
    }

    suspend fun setWakeTime(time: LocalTime) {
        val current = dao.programOnce() ?: return
        dao.upsertProgram(current.copy(wakeTime = time))
    }

    /** Calcule les repères de la semaine d'observation pour l'écran de transition vers la restriction. */
    suspend fun baselineTransitionInfo(): BaselineTransitionInfo? {
        val program = dao.programOnce() ?: return null
        val screening = evaluator.evaluate(program.answers())
        val profile = RestrictionProfile.from(config, screening)
        val entries = dao.entriesOnce().map { it.toModel() }
        val baselineEnd = program.startDate.plusDays(config.baselineDays.toLong())
        val baselineNights = entries.filter { it.nightOf >= program.startDate && it.nightOf < baselineEnd }
        val summary = PeriodSummary.of(baselineNights) ?: PeriodSummary.of(entries) ?: return null
        val suggested = calculator.suggestWakeTime(baselineNights.ifEmpty { entries })
        val rawMinutes = ((summary.meanTotalSleepTime.toMinutes() + 14) / 15) * 15
        val rawDuration = Duration.ofMinutes(rawMinutes)
        val duration = if (profile != null) {
            rawDuration.coerceAtLeast(profile.minimumWindow).coerceAtMost(config.maximumWindow)
        } else {
            rawDuration
        }
        return BaselineTransitionInfo(
            summary = summary,
            profile = profile,
            suggestedWakeTime = suggested,
            prescribedDuration = duration,
        )
    }

    suspend fun entry(nightOf: LocalDate): DiaryEntry? = dao.entry(nightOf)?.toModel()

    suspend fun previousEntry(nightOf: LocalDate): DiaryEntry? = dao.entryBefore(nightOf)?.toModel()

    suspend fun saveEntry(entry: DiaryEntry) = dao.upsertEntry(entry.toEntity())

    /** Restaure les nuits issues d'un fichier CSV exporté par Sommelle et recalcule les fenêtres. */
    suspend fun importCsv(content: String): Int {
        val parsed = DiaryCsv.parse(content)
        parsed.forEach { dao.upsertEntry(it.toEntity()) }
        if (parsed.isNotEmpty()) synchronize()
        return parsed.size
    }

    /**
     * Enregistre les fenêtres dues (initiale et ajustements hebdomadaires, y compris en rattrapage
     * après plusieurs semaines sans ouvrir l'appli), puis renvoie l'étape en cours.
     */
    suspend fun synchronize(): WindowStep? {
        val program = dao.programOnce() ?: return null
        val screening = evaluator.evaluate(program.answers())
        repeat(MAX_CATCH_UP_WEEKS) {
            val step = engine.windowStep(
                today = today(),
                startDate = program.startDate,
                screening = screening,
                entries = dao.entriesOnce().map { it.toModel() },
                windows = dao.windowsOnce().map { it.toModel().window },
                wakeTime = program.wakeTime,
            )
            when (step) {
                is WindowStep.NewInitialWindow -> dao.upsertWindow(step.window.toEntity(change = null))
                is WindowStep.NewWeeklyDecision -> dao.upsertWindow(step.decision.window.toEntity(step.decision.change))
                else -> return step
            }
        }
        return null
    }

    // Réglages des rappels

    val reminderSettings: Flow<ReminderSettings> = dao.settings().map { it?.toModel() ?: ReminderSettings() }

    suspend fun reminderSettingsOnce(): ReminderSettings = dao.settingsOnce()?.toModel() ?: ReminderSettings()

    suspend fun saveReminderSettings(settings: ReminderSettings) = dao.upsertSettings(
        SettingsEntity(
            morningReminder = settings.morningEnabled,
            morningTime = settings.morningTime,
            eveningReminder = settings.eveningEnabled,
        ),
    )

    // Modules cognitifs

    val completedModules: Flow<Set<CognitiveModule>> = dao.moduleProgress().map { rows ->
        rows.mapNotNull { row -> CognitiveModule.entries.firstOrNull { it.name == row.module } }.toSet()
    }

    suspend fun completeModule(module: CognitiveModule) =
        dao.upsertModuleProgress(ModuleProgressEntity(module = module.name, completedOn = today()))

    // Ressenti de la journée

    val daytimeImpacts: Flow<List<DaytimeImpact>> = dao.daytimeImpacts().map { rows -> rows.map { DaytimeImpact(it.date, it.score) } }

    suspend fun daytimeImpactsOnce(): List<DaytimeImpact> = dao.daytimeImpactsOnce().map { DaytimeImpact(it.date, it.score) }

    suspend fun saveDaytimeImpact(score: Int) = dao.upsertDaytimeImpact(DaytimeImpactEntity(date = today(), score = score))

    /** Effacement complet, en un geste depuis les réglages (documentation/architecture.md, section « Données »). */
    suspend fun eraseEverything() {
        dao.deleteWindows()
        dao.deleteEntries()
        dao.deleteProgram()
        dao.deleteSettings()
        dao.deleteModuleProgress()
        dao.deleteDaytimeImpacts()
    }

    private fun SettingsEntity.toModel() = ReminderSettings(
        morningEnabled = morningReminder,
        morningTime = morningTime,
        eveningEnabled = eveningReminder,
    )

    companion object {
        private const val MAX_CATCH_UP_WEEKS = 104

        fun create(context: Context) = SommelleRepository(SommelleDatabase.create(context).dao())
    }
}
