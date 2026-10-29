package com.azerostudio.kadan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/*
 * Fires the weekday 15:15 WIB PR capture nudge, then chains the next one.
 * The chain is the schedule: there is no repeating alarm, because a
 * repeating alarm cannot skip Saturday and Sunday.
 */
class PrNudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showPrNudge(context)
        ReminderScheduler.schedulePrNudge(context)
    }
}
