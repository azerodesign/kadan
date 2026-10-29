package com.azerostudio.kadan.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.azerostudio.kadan.MainActivity

/*
 * All Kadan notifications go through here, on one channel.
 * One channel (not one per feature) keeps the settings surface simple for
 * a personal app: the user either wants Kadan reminders or does not.
 * Importance HIGH is deliberate: a due reminder that arrives silently in
 * the notification shade has already failed at its one job.
 *
 * Small icon: the platform info icon is used in v1 because Kadan has no
 * launcher icon asset yet. Replace setSmallIcon with the Kadan icon the
 * moment an icon asset exists — a notification without a recognisable icon
 * is easy to dismiss by accident.
 */
object NotificationHelper {
    const val CHANNEL_ID = "kadan_reminders"
    const val EXTRA_OPEN_CAPTURE = "kadan_open_capture"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pengingat Kadan",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Pengingat tugas dan nudge capture PR Kadan."
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showDueReminder(context: Context, taskId: Long, title: String, categoryLabel: String) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            taskId.toInt(),
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Jatuh tempo hari ini · $categoryLabel")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(taskId.toInt(), notification)
        } catch (_: SecurityException) {
            // Permission revoked concurrently.
        }
    }

    fun showPrNudge(context: Context) {
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_OPEN_CAPTURE, true)
            // Pre-select the PR category in the capture sheet.
            putExtra("kadan_capture_category", "PR")
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            ReminderScheduler.PR_NUDGE_REQUEST_CODE,
            intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Hari ini ada PR apa?")
            .setContentText("Catat PR hari ini sebelum keburu lupa — cuma butuh 10 detik.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(
                ReminderScheduler.PR_NUDGE_REQUEST_CODE,
                notification
            )
        } catch (_: SecurityException) {
            // Permission revoked concurrently.
        }
    }
}
