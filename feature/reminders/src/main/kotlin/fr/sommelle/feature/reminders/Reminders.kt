package fr.sommelle.feature.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import fr.sommelle.core.data.SommelleRepository
import fr.sommelle.core.data.SommelleRepositoryProvider
import fr.sommelle.core.presentation.AppLinks
import fr.sommelle.core.presentation.Texts
import fr.sommelle.core.presentation.ReminderKind
import fr.sommelle.core.presentation.ReminderPlanner
import fr.sommelle.core.presentation.ReminderSettings
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Planification des rappels. Chaque rappel est une tâche unique, replanifiée après chaque envoi
 * et à chaque changement de réglages ou de fenêtre. WorkManager la conserve après un redémarrage.
 */
object ReminderScheduler {

    private fun workName(kind: ReminderKind) = "rappel-${kind.name.lowercase()}"

    /** Recalcule les deux rappels à partir de l'état enregistré. */
    suspend fun reschedule(context: Context, repository: SommelleRepository) {
        val settings = repository.reminderSettingsOnce()
        val snapshot = repository.snapshot.first()
        val wakeTime = snapshot?.program?.wakeTime
        val window = snapshot?.windows?.maxByOrNull { it.window.effectiveFrom }?.window
        val now = LocalDateTime.of(repository.today(), repository.now())

        plan(context, ReminderKind.MORNING, if (snapshot != null && settings.morningEnabled) ReminderPlanner.morningTime(settings, wakeTime) else null, now)
        plan(context, ReminderKind.EVENING, if (settings.eveningEnabled) ReminderPlanner.eveningTime(window) else null, now)
    }

    private fun plan(context: Context, kind: ReminderKind, time: LocalTime?, now: LocalDateTime) {
        val manager = WorkManager.getInstance(context)
        if (time == null) {
            manager.cancelUniqueWork(workName(kind))
            return
        }
        val delay = Duration.between(now, ReminderPlanner.next(now, time))
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay)
            .setInputData(workDataOf(ReminderWorker.KIND to kind.name))
            .build()
        manager.enqueueUniqueWork(workName(kind), ExistingWorkPolicy.REPLACE, request)
    }

    fun isEnabled(settings: ReminderSettings) = settings.morningEnabled || settings.eveningEnabled
}

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as? SommelleRepositoryProvider)?.repository ?: return Result.failure()
        val kind = inputData.getString(KIND)?.let { name -> ReminderKind.entries.firstOrNull { it.name == name } } ?: return Result.failure()
        val snapshot = repository.snapshot.first()

        if (snapshot != null) {
            when (kind) {
                ReminderKind.MORNING -> {
                    // Le bilan de la semaine se calcule d'habitude à l'ouverture de l'appli :
                    // on le fait ici pour pouvoir l'annoncer, c'est le meilleur moment du programme.
                    val windowsBefore = snapshot.windows.size
                    repository.synchronize()
                    val windowsAfter = repository.snapshot.first()?.windows?.size ?: windowsBefore
                    if (windowsBefore > 0 && windowsAfter > windowsBefore) {
                        notify(
                            WEEK_NOTIFICATION_ID,
                            applicationContext.getString(R.string.reminder_week_title),
                            applicationContext.getString(R.string.reminder_week_text),
                            AppLinks.WEEK,
                        )
                    }
                    // Pas de rappel si la nuit est déjà notée.
                    val filled = snapshot.entries.any { it.nightOf == repository.today() }
                    if (!filled) {
                        notify(
                            kind.ordinal + 1,
                            applicationContext.getString(R.string.reminder_morning_title),
                            applicationContext.getString(R.string.reminder_morning_text),
                            AppLinks.DIARY,
                        )
                    }
                }
                ReminderKind.EVENING -> {
                    val window = snapshot.windows.maxByOrNull { it.window.effectiveFrom }?.window
                    if (window != null) {
                        notify(
                            kind.ordinal + 1,
                            applicationContext.getString(R.string.reminder_evening_title, Texts.current().time(window.bedTime)),
                            applicationContext.getString(R.string.reminder_evening_text),
                            link = null,
                        )
                    }
                }
            }
        }
        ReminderScheduler.reschedule(applicationContext, repository)
        return Result.success()
    }

    // L'autorisation est vérifiée au début de la fonction.
    @SuppressLint("MissingPermission")
    private fun notify(id: Int, title: String, text: String, link: String?) {
        val context = applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureChannel(context)
        // Le rappel du matin ouvre directement le journal de la nuit, le bilan ouvre la semaine.
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            ?.apply { if (link != null) putExtra(AppLinks.EXTRA_OPEN, link) }
        val pending = launch?.let {
            PendingIntent.getActivity(context, id, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context).notify(id, notification)
    }

    companion object {
        const val KIND = "kind"
        private const val WEEK_NOTIFICATION_ID = 10
        private const val CHANNEL_ID = "rappels"

        fun ensureChannel(context: Context) {
            val manager = context.getSystemService(NotificationManager::class.java) ?: return
            val channel = NotificationChannel(CHANNEL_ID, context.getString(R.string.reminders_channel), NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = context.getString(R.string.reminders_channel_description)
            manager.createNotificationChannel(channel)
        }
    }
}
