package fr.sommelle

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import fr.sommelle.core.presentation.AppLinks
import android.widget.RemoteViews
import fr.sommelle.core.data.ProgramSnapshot
import fr.sommelle.core.data.SommelleRepositoryProvider
import fr.sommelle.core.presentation.Texts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Widget discret d'écran d'accueil : rappelle la fenêtre de sommeil du soir
 * et ouvre directement l'application au réveil pour noter la nuit en moins d'une minute.
 */
class SommelleWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val provider = context.applicationContext as? SommelleRepositoryProvider ?: return
        // goAsync garde le récepteur en vie jusqu'à la fin de la lecture en base.
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val snapshot = provider.repository.snapshot.first()
                updateWidgets(context, appWidgetManager, appWidgetIds, snapshot)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun refreshAll(context: Context, snapshot: ProgramSnapshot?) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, SommelleWidget::class.java))
            if (ids.isNotEmpty()) {
                updateWidgets(context, manager, ids, snapshot)
            }
        }

        private fun updateWidgets(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            snapshot: ProgramSnapshot?,
        ) {
            val latestWindow = snapshot?.windows?.maxByOrNull { it.window.effectiveFrom }?.window
            val texts = Texts.current()
            val primaryText = if (latestWindow != null) {
                texts.windowRange(texts.time(latestWindow.bedTime), texts.time(latestWindow.wakeTime))
            } else {
                context.getString(R.string.widget_observation)
            }

            // Nuit pas encore notée : le widget ouvre directement le journal, comme promis.
            val nightFilled = snapshot?.entries?.any { it.nightOf == java.time.LocalDate.now() } ?: true
            val launchIntent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .apply { if (!nightFilled) putExtra(AppLinks.EXTRA_OPEN, AppLinks.DIARY) }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.sommelle_widget).apply {
                    setTextViewText(R.id.widget_title, context.getString(R.string.widget_window_title))
                    setTextViewText(R.id.widget_primary, primaryText)
                    setTextViewText(R.id.widget_secondary, context.getString(R.string.widget_tap_hint))
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }
                manager.updateAppWidget(id, views)
            }
        }
    }
}
