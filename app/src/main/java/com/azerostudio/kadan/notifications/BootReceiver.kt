package com.azerostudio.kadan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.azerostudio.kadan.data.KadanDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Alarms do not survive a reboot. After BOOT_COMPLETED this receiver
 * re-schedules every not-done task's due reminder and re-seeds the
 * weekday PR nudge chain. Without this, a single reboot would silently
 * delete every reminder — the worst failure mode for a reminder app.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val tasks = KadanDatabase.get(context).taskDao().getAll()
                for (task in tasks) {
                    ReminderScheduler.scheduleDue(context, task)
                }
                ReminderScheduler.schedulePrNudge(context)
            } finally {
                pending.finish()
            }
        }
    }
}
