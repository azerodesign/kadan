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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.azerostudio.kadan.ui.theme.ThemeMode
import com.azerostudio.kadan.ui.theme.ThemePreferences
import com.azerostudio.kadan.widget.KadanWidgetProvider
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.azerostudio.kadan.data.KadanDatabase
import com.azerostudio.kadan.data.TaskRepository
import com.azerostudio.kadan.notifications.NotificationHelper
import com.azerostudio.kadan.notifications.ReminderScheduler
import com.azerostudio.kadan.ui.KadanViewModel
import com.azerostudio.kadan.ui.HomeScreen
import com.azerostudio.kadan.ui.theme.KadanTheme

/*
 * MainActivity — the single activity of Kadan v1.4.
 *
 * v1.2 additions (kept in v1.3/v1.4): theme mode is read from DataStore (ThemePreferences)
 * and resolved here, and every task save/remove also refreshes the
 * Hari Ini home widget so the widget never shows stale counts.
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
                        KadanWidgetProvider.requestUpdate(applicationContext)
                    },
                    onTaskRemoved = { taskId ->
                        ReminderScheduler.cancelDue(applicationContext, taskId)
                        KadanWidgetProvider.requestUpdate(applicationContext)
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
            val themeMode by ThemePreferences.themeModeFlow(applicationContext)
                .collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            KadanTheme(darkTheme = darkTheme) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenExactAlarmSettings = {
                        ReminderScheduler.openExactAlarmSettings(this)
                    },
                    exactAlarmGranted = exactAlarmGranted,
                    themeMode = themeMode,
                    onThemeModeChange = { mode ->
                        lifecycleScope.launch {
                            ThemePreferences.setThemeMode(applicationContext, mode)
                        }
                    }
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
        KadanWidgetProvider.requestUpdate(this)
    }

    private fun handleCaptureIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_CAPTURE, false) == true) {
            val category = intent.getStringExtra("kadan_capture_category") ?: "PR"
            viewModel.openCapture(category)
        }
    }
}
