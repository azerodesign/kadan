package com.azerostudio.kadan

import android.app.Application
import com.azerostudio.kadan.notifications.NotificationHelper
import com.azerostudio.kadan.notifications.ReminderScheduler

/*
 * Application entry point.
 * Creates the notification channel and seeds the weekday PR nudge chain
 * on every launch. Seeding is idempotent: schedulePrNudge() computes the
 * next weekday 15:15 and replaces the same PendingIntent, so calling it
 * on every launch cannot double-schedule the nudge.
 */
class KadanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannel(this)
        ReminderScheduler.schedulePrNudge(this)
    }
}
