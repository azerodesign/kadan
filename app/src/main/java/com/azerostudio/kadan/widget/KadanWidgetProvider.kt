package com.azerostudio.kadan.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.azerostudio.kadan.MainActivity
import com.azerostudio.kadan.R
// Note: R is imported explicitly because this class lives in the
// .widget subpackage, not the app root package.
import com.azerostudio.kadan.data.KadanDatabase
import com.azerostudio.kadan.notifications.NotificationHelper
import com.azerostudio.kadan.ui.KadanViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

/*
 * Kadan v1.2 home widget — "Hari Ini", minimal on purpose.
 *
 * What it shows, and nothing more: how many tasks are due today, how
 * many are overdue, how many are unfinished, and the next unfinished
 * task by due date/time. A "Tambah" button opens the app straight into
 * the capture sheet (same intent the PR nudge uses).
 *
 * What it does NOT do: no periodic polling (updatePeriodMillis = 0, so
 * it costs no battery in the background). It refreshes when the system
 * asks (onUpdate) and whenever MainActivity saves/removes a task, via
 * requestUpdate(). If the database read fails, the widget falls back to
 * "Buka Kadan" instead of showing invented numbers — a widget must
 * never display counts it did not actually read.
 *
 * RemoteViews, not Glance/Compose: a home widget cannot host Compose
 * UI. The layout is plain TextViews in res/layout/widget_kadan.xml.
 * Colours there are fixed dark-on-light-independent values in that
 * layout only, because RemoteViews cannot read the Compose theme.
 */
class KadanWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = KadanDatabase.get(context).taskDao()
                val tasks = dao.getAll()
                val today = LocalDate.now(KadanViewModel.ZONE).toEpochDay()
                val dueToday = tasks.count { !it.isDone && it.dueDate == today }
                val overdue = tasks.count {
                    !it.isDone && it.dueDate < today
                }
                val unfinished = tasks.count { !it.isDone }
                val next = tasks.filter { !it.isDone }
                    .sortedWith(compareBy({ it.dueDate }, { it.dueTimeMinutes ?: 480 }))
                    .firstOrNull()
                val views = RemoteViews(context.packageName, R.layout.widget_kadan)
                views.setTextViewText(R.id.widget_title, "Kadan — Hari Ini")
                views.setTextViewText(R.id.widget_counts, "$dueToday hari ini · $overdue telat · $unfinished belum selesai")
                views.setTextViewText(
                    R.id.widget_next,
                    if (next != null) "Berikutnya: ${next.title}" else "Tidak ada tugas berikutnya"
                )
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(NotificationHelper.EXTRA_OPEN_CAPTURE, true)
                    putExtra("kadan_capture_category", "PR")
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_add, pendingIntent)
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
                // Fallback, not invented data: open the app to see real counts.
                val views = RemoteViews(context.packageName, R.layout.widget_kadan)
                views.setTextViewText(R.id.widget_title, "Kadan — Hari Ini")
                views.setTextViewText(R.id.widget_counts, "Buka Kadan untuk melihat tugas hari ini")
                views.setTextViewText(R.id.widget_next, "")
                for (id in appWidgetIds) {
                    appWidgetManager.updateAppWidget(id, views)
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, KadanWidgetProvider::class.java)
            )
            if (ids.isNotEmpty()) {
                val intent = Intent(context, KadanWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
