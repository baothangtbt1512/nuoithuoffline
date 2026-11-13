package com.bubududu.pet.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bubududu.pet.MainActivity
import com.bubududu.pet.R
import com.bubududu.pet.data.PetRepository
import com.bubududu.pet.model.TemporaryAction
import com.bubududu.pet.ui.Pet2DRenderer
import java.util.concurrent.TimeUnit

class PetWidgetProvider : AppWidgetProvider() {

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        schedulePeriodicRefresh(context)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        schedulePeriodicRefresh(context)
        val repo = PetRepository(context)
        val state = repo.loadState()
        val snapshot = repo.resolveRoutine(state)
        val bmp = Pet2DRenderer.renderWidgetSceneBitmap(context, state, snapshot)

        for (widgetId in appWidgetIds) {
            val views = buildRemoteViews(context, state, snapshot.routine.shortTitle, snapshot.routine.statusLine, bmp)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action !in setOf(ACTION_FEED, ACTION_CLEAN, ACTION_PAT)) return

        val repo = PetRepository(context)
        val current = repo.loadState()
        val updated = when (action) {
            ACTION_FEED -> repo.feedPet(current)
            ACTION_CLEAN -> repo.cleanRoomAndBrush(current)
            ACTION_PAT -> repo.patPet(current)
            else -> current
        }
        val tempAction = when (action) {
            ACTION_FEED -> TemporaryAction.FEED
            ACTION_CLEAN -> TemporaryAction.BRUSH_CLEAN
            ACTION_PAT -> TemporaryAction.PAT
            else -> null
        }
        updateAllWidgets(context, updated, tempAction)
    }

    companion object {
        const val ACTION_FEED = "com.bubududu.pet.WIDGET_FEED"
        const val ACTION_CLEAN = "com.bubududu.pet.WIDGET_CLEAN"
        const val ACTION_PAT = "com.bubududu.pet.WIDGET_PAT"
        private const val WORK_NAME = "bubu_dudu_pet_widget_refresh"

        fun schedulePeriodicRefresh(context: Context) {
            val request = PeriodicWorkRequestBuilder<PetUpdateWorker>(30, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun updateAllWidgets(
            context: Context,
            explicitState: com.bubududu.pet.model.PetState? = null,
            tempAction: TemporaryAction? = null
        ) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, PetWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isEmpty()) return

            val repo = PetRepository(context)
            val state = explicitState ?: repo.loadState()
            val snapshot = repo.resolveRoutine(state, tempAction)
            val caption = tempAction?.bubbleText ?: snapshot.routine.statusLine
            val bmp = Pet2DRenderer.renderWidgetSceneBitmap(context, state, snapshot)

            for (id in ids) {
                val views = buildRemoteViews(context, state, snapshot.routine.shortTitle, caption, bmp)
                manager.updateAppWidget(id, views)
            }
        }

        private fun buildRemoteViews(
            context: Context,
            state: com.bubududu.pet.model.PetState,
            routineTitle: String,
            caption: String,
            sceneBitmap: android.graphics.Bitmap
        ): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.pet_widget)
            views.setTextViewText(R.id.widget_title, "Bubu & Dudu • $routineTitle")
            views.setTextViewText(R.id.widget_stats_badge, "Lv.${state.level} • ${state.coins} xu")
            views.setTextViewText(R.id.widget_mood_note, caption)
            views.setTextViewText(R.id.widget_bar_hunger, "🍚 No: ${state.hunger}%")
            views.setTextViewText(R.id.widget_bar_happy, "💖 Vui: ${state.happiness}%")
            views.setTextViewText(R.id.widget_bar_clean, "✨ Sạch: ${state.hygiene}%")
            views.setImageViewBitmap(R.id.widget_pet_image, sceneBitmap)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                10,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)
            views.setOnClickPendingIntent(R.id.widget_pet_image, openPending)

            views.setOnClickPendingIntent(R.id.widget_btn_feed, actionPending(context, ACTION_FEED, 11))
            views.setOnClickPendingIntent(R.id.widget_btn_clean, actionPending(context, ACTION_CLEAN, 12))
            views.setOnClickPendingIntent(R.id.widget_btn_pat, actionPending(context, ACTION_PAT, 13))
            return views
        }

        private fun actionPending(context: Context, action: String, reqCode: Int): PendingIntent {
            val intent = Intent(context, PetWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
