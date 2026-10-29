package com.azerostudio.kadan.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.azerostudio.kadan.data.KadanDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/*
 * Fires when a due-task alarm goes off.
 * It re-checks the database before showing anything: if the task was
 * deleted or marked done between scheduling and firing, no stale
 * notification is shown. That check is why this receiver uses goAsync()
 * with a coroutine instead of blocking the main thread.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("task_id", -1L)
        val title = intent.getStringExtra("title") ?: return
        val category = intent.getStringExtra("category") ?: ""
        if (taskId == -1L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = KadanDatabase.get(context).taskDao().getById(taskId)
                if (task != null && !task.isDone) {
                    NotificationHelper.showDueReminder(context, taskId, title, category)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
