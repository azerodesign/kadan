package com.azerostudio.kadan.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.azerostudio.kadan.data.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/*
 * How Kadan reminders are scheduled — read this before touching it.
 *
 * Mechanism: AlarmManager, NOT WorkManager, for due reminders. WorkManager
 * is for deferrable work; a reminder that must fire at a set time is
 * exactly what AlarmManager is for. WorkManager is therefore NOT used in
 * v1, even though it is on the classpath for a future use.
 *
 * Exact alarms (Android 12+, API 31+):
 *  - SCHEDULE_EXACT_ALARM is a special permission. canScheduleExactAlarms()
 *    must be checked, and if it is false the user must be sent to the
 *    system "Alarms & reminders" screen (ACTION_REQUEST_SCHEDULE_EXACT_ALARM).
 *  - If exact alarms are denied, scheduleDue() falls back to
 *    setAndAllowWhileIdle() — an INEXACT alarm. The app must never claim
 *    an exact reminder is set in that state; MainActivity surfaces the
 *    settings screen instead of silently accepting the fallback.
 *
 * Timezone: all times are Asia/Jakarta local time, because the locked
 * PR nudge is "weekdays 15:15 WIB". Never use UTC or the device default
 * silently here — a phone that travels would otherwise shift the nudge.
 *
 * PR nudge: a chain, not a repeating alarm. PrNudgeReceiver shows the
 * nudge and then calls schedulePrNudge() again, which computes the NEXT
 * weekday 15:15. A repeating alarm cannot skip Saturday/Sunday, so a
 * chain is the correct shape. BootReceiver also re-seeds the chain.
 *
 * Due reminders: one exact alarm per task, keyed by the task id. Cancelling
 * uses the same PendingIntent (same request code + same intent action),
 * which is why cancelDue() must mirror scheduleDue() exactly.
 *
 * MIUI / Redmi caveat (see README, and do not soften it): Xiaomi's battery
 * optimisation can kill or defer alarms for apps that are not whitelisted,
 * regardless of this code being correct. On-device testing on a Redmi
 * phone — with the app whitelisted from battery optimisation — is required
 * before anyone claims reminders "work". A VM build passing says nothing
 * about this.
 */
object ReminderScheduler {
    const val PR_NUDGE_REQUEST_CODE = 900001
    private const val ACTION_DUE = "com.azerostudio.kadan.ACTION_DUE"
    private const val ACTION_PR_NUDGE = "com.azerostudio.kadan.ACTION_PR_NUDGE"
    private const val EXTRA_TASK_ID = "task_id"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_CATEGORY = "category"

    private val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    private fun alarmManager(context: Context): AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    private fun dueIntent(context: Context, task: Task): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DUE
            putExtra(EXTRA_TASK_ID, task.id)
            putExtra(EXTRA_TITLE, task.title)
            putExtra(EXTRA_CATEGORY, task.category)
        }
        return PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun prNudgeIntent(context: Context): PendingIntent {
        val intent = Intent(context, PrNudgeReceiver::class.java).apply {
            action = ACTION_PR_NUDGE
        }
        return PendingIntent.getBroadcast(
            context,
            PR_NUDGE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Schedules the due reminder for [task].
     * @return true if an exact alarm was set, false if it fell back to
     * inexact scheduling (exact alarms denied) or if nothing was scheduled
     * (task done, or due time already past — overdue tasks are shown in
     * the list, not re-notified for a past moment).
     */
    fun scheduleDue(context: Context, task: Task): Boolean {
        if (task.isDone) return false
        val dueDate = LocalDate.ofEpochDay(task.dueDate)
        // No time set = due that day: fire at 08:00 WIB that morning, so
        // the user hears about it before the day is over, not at midnight.
        val time = task.dueTimeMinutes?.let { LocalTime.of(it / 60, it % 60) }
            ?: LocalTime.of(8, 0)
        val trigger = ZonedDateTime.of(LocalDateTime.of(dueDate, time), ZONE)
        if (trigger.isBefore(ZonedDateTime.now(ZONE))) return false
        val exact = canScheduleExactAlarms(context)
        if (exact) {
            alarmManager(context).setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                dueIntent(context, task)
            )
        } else {
            alarmManager(context).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                trigger.toInstant().toEpochMilli(),
                dueIntent(context, task)
            )
        }
        return exact
    }

    fun cancelDue(context: Context, taskId: Long) {
        // Mirror scheduleDue's PendingIntent exactly, then cancel it.
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_DUE
        }
        val pending = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager(context).cancel(pending)
    }

    /**
     * Schedules the next weekday (Mon–Fri) 15:15 WIB PR capture nudge.
     * If today is a weekday and 15:15 has not passed, that is today;
     * otherwise it is the next weekday. Saturday/Sunday are skipped.
     */
    fun schedulePrNudge(context: Context) {
        var date = LocalDate.now(ZONE)
        var candidate = ZonedDateTime.of(date, LocalTime.of(15, 15), ZONE)
        while (candidate.dayOfWeek == DayOfWeek.SATURDAY ||
            candidate.dayOfWeek == DayOfWeek.SUNDAY ||
            candidate.isBefore(ZonedDateTime.now(ZONE))
        ) {
            date = date.plusDays(1)
            candidate = ZonedDateTime.of(date, LocalTime.of(15, 15), ZONE)
        }
        val exact = canScheduleExactAlarms(context)
        if (exact) {
            alarmManager(context).setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                candidate.toInstant().toEpochMilli(),
                prNudgeIntent(context)
            )
        } else {
            alarmManager(context).setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                candidate.toInstant().toEpochMilli(),
                prNudgeIntent(context)
            )
        }
    }
}
