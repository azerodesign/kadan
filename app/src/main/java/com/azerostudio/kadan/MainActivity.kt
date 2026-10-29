package com.azerostudio.kadan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.azerostudio.kadan.data.KadanDatabase
import com.azerostudio.kadan.data.TaskRepository
import com.azerostudio.kadan.notifications.NotificationHelper
import com.azerostudio.kadan.notifications.ReminderScheduler
import com.azerostudio.kadan.ui.HomeScreen
import com.azerostudio.kadan.ui.KadanViewModel
import com.azerostudio.kadan.ui.theme.KadanTheme

/*
 * MainActivity — the single activity of Kadan v1.
 *
 * Permissions, in order, and why each is requested where it is:
 *  1. POST_NOTIFICATIONS (Android 13+): requested once, on first launch,
 *     via the standard runtime permission flow. Without it, no reminder
 *     can ever be shown — so this is not optional polish.
 *  2. SCHEDULE_EXACT_ALARM (Android 12+): NOT requested as a runtime
 *     permission, because it is not one — it is a system setting. If it
 *     is denied, HomeScreen shows a banner with a button that opens the
 *     settings screen (ReminderScheduler.openExactAlarmSettings). The
 *     app never silently pretends an exact reminder is set when it is not.
 *
 * Capture from a notification: the PR nudge PendingIntent launches this
 * activity with EXTRA_OPEN_CAPTURE=true and a pre-selected category.
 * onNewIntent forwards that to the ViewModel so the sheet opens even if
 * the app was already running (launchMode=singleTop).
 */
class MainActivity : ComponentActivity() {

    private var exactAlarmGranted by mutableStateOf(true)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Result is not gated on: if denied, notifications simply cannot
           be shown, and the user can re-enable them in system settings.
           The app must not loop-request the permission. */ }

    private val viewModel: KadanViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repository = TaskRepository(KadanDatabase.get(applicationContext).taskDao())
                return KadanViewModel(
                    repository = repository,
                    onTaskSaved = { task ->
                        ReminderScheduler.scheduleDue(applicationContext, task)
                    },
                    onTaskRemoved = { taskId ->
                        ReminderScheduler.cancelDue(applicationContext, taskId)
                    }
                ) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        exactAlarmGranted = ReminderScheduler.canScheduleExactAlarms(this)
        handleCaptureIntent(intent)

        setContent {
            KadanTheme {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenExactAlarmSettings = {
                        ReminderScheduler.openExactAlarmSettings(this)
                    },
                    exactAlarmGranted = exactAlarmGranted
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCaptureIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // Re-check on return from the system alarm settings screen, so
        // the banner disappears without requiring an app restart.
        exactAlarmGranted = ReminderScheduler.canScheduleExactAlarms(this)
    }

    private fun handleCaptureIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CAPTURE, false) == true) {
            val category = intent.getStringExtra("kadan_capture_category") ?: "PR"
            viewModel.openCapture(category)
        }
    }
}
