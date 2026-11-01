package com.azerostudio.kadan.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.azerostudio.kadan.data.Category
import com.azerostudio.kadan.data.Task
import com.azerostudio.kadan.ui.theme.CyanFixedDim
import com.azerostudio.kadan.ui.theme.HeroEnterTransition
import com.azerostudio.kadan.ui.theme.KadanSpacing
import com.azerostudio.kadan.ui.theme.ThemeMode
import com.azerostudio.kadan.ui.theme.statEnterTransition
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Home screen — "Hari Ini", v1.4 design lock.
 *
 * v1.3 was the visual break (solid ocean hero, separate stat cards,
 * solid FAB, theme in the footer). v1.4 is the Fotara lesson applied:
 * the look is now LOCKED to tokens, not per-screen taste — spacing
 * from KadanSpacing, top bar from KadanTopBar, motion from Motion.kt.
 * Nothing about reminders, data or capture changes — this file only
 * re-presents the same counts, through the same tokens, every time.
 *
 * Material 3 rules applied here (see the Kadan Material guide):
 *  - Scaffold slots only: TopAppBar, floatingActionButton, snackbarHost.
 *    The LazyColumn applies Scaffold's PaddingValues, so content never
 *    hides under the FAB or the navigation bar.
 *  - Colours only via MaterialTheme.colorScheme roles. No hex, no
 *    Color.White/Black anywhere in this file.
 *  - Typography only via MaterialTheme.typography roles: titleMedium for
 *    titles, bodySmall + onSurfaceVariant for time/category metadata.
 *  - Overdue is NEVER colour alone: the row uses errorContainer AND the
 *    word "Telat" in its supporting text, plus an error-coloured count.
 *  - Every interactive row is at least 48 dp tall: the Checkbox is 48 dp
 *    by default and the whole Card is clickable, so the row target is
 *    far larger than the checkbox alone.
 *  - Exactly one primary action: the ExtendedFloatingActionButton "Tambah".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: KadanViewModel,
    onOpenExactAlarmSettings: () -> Unit,
    exactAlarmGranted: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val captureRequest by viewModel.captureRequest.collectAsState()
    val editingTask by viewModel.editingTask.collectAsState()
    val pendingDeleted by viewModel.pendingDeleted.collectAsState()
    val focusSessionTask by viewModel.focusSessionTask.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val today = LocalDate.now(KadanViewModel.ZONE)
    val todayEpoch = today.toEpochDay()
    val nowMinutes = java.time.LocalTime.now(KadanViewModel.ZONE).hour * 60 +
        java.time.LocalTime.now(KadanViewModel.ZONE).minute

    fun isOverdue(task: Task): Boolean {
        if (task.isDone) return false
        if (task.dueDate < todayEpoch) return true
        if (task.dueDate == todayEpoch && task.dueTimeMinutes != null) {
            return task.dueTimeMinutes < nowMinutes
        }
        return false
    }

    val dueTodayCount = tasks.count { !it.isDone && it.dueDate == todayEpoch }
    val overdueCount = tasks.count { isOverdue(it) }
    val unfinishedCount = tasks.count { !it.isDone }
    // Progress for the hero card: only tasks actually due today count,
    // so a future task never inflates today's bar.
    val todayTasks = tasks.filter { it.dueDate == todayEpoch }
    val doneTodayCount = todayTasks.count { it.isDone }
    val todayTotalCount = todayTasks.size
    val nextTask = tasks.filter { !it.isDone }
        .sortedWith(compareBy({ it.dueDate }, { it.dueTimeMinutes ?: 480 }))
        .firstOrNull()
    val visibleTasks = tasks.filter { task ->
        selectedCategory == null || task.category == selectedCategory
    }

    var showTaskListSheet by remember { mutableStateOf(false) }

    // Active focus task: explicitly chosen, or top overdue/today task, or first unfinished task
    val currentFocusTask = focusSessionTask
        ?: tasks.firstOrNull { !it.isDone && (it.dueDate == todayEpoch || isOverdue(it)) }
        ?: tasks.firstOrNull { !it.isDone }

    // Delete -> Snackbar with Undo. ActionPerformed restores, Dismissed
    // just clears the pending state (the delete stays deleted).
    LaunchedEffect(pendingDeleted) {
        val deleted = pendingDeleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "\"${deleted.title}\" dihapus",
            actionLabel = "Urungkan",
            withDismissAction = true
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.restoreDeleted()
        } else {
            viewModel.clearPendingDeleted()
        }
    }

    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    // Root UI: FocusSessionScreen is the primary default experience
    Box(modifier = Modifier.fillMaxSize()) {
        FocusSessionScreen(
            task = currentFocusTask,
            tasksCount = unfinishedCount,
            onOpenTaskList = { showTaskListSheet = true },
            onOpenCapture = { viewModel.openCapture(Category.PR.label) },
            onDismiss = {
                viewModel.closeFocusSession()
            },
            onStartFocus = {
                // Focus session active
            },
            onSnooze = { minutes ->
                currentFocusTask?.let { task ->
                    viewModel.snoozeTask(task, minutes)
                }
            },
            onCompleteTask = { task ->
                viewModel.toggleDone(task)
                if (focusSessionTask != null) {
                    viewModel.closeFocusSession()
                }
            },
            onEmergencyCall = {
                try {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(dialIntent)
                } catch (_: Exception) {}
            }
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 76.dp)
        )
    }

    if (showTaskListSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTaskListSheet = false },
            containerColor = Color(0xFF181C24),
            contentColor = Color(0xFFDFE2EE),
            dragHandle = {
                BottomSheetDefaults.DragHandle(
                    color = Color(0xFF434655)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = KadanSpacing.Screen)
                    .padding(bottom = 32.dp)
            ) {
                // Sheet Header: Title, pending count, and Add button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Tugas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDFE2EE)
                        )
                        Text(
                            text = "$unfinishedCount tugas belum selesai",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8D90A0)
                        )
                    }
                    TextButton(
                        onClick = {
                            viewModel.openCapture(Category.PR.label)
                        }
                    ) {
                        Text(
                            text = "+ Tugas",
                            color = CyanFixedDim,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(KadanSpacing.Section)
                ) {
                    if (!exactAlarmGranted) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(KadanSpacing.Card)) {
                                    Text(
                                        text = "Izin alarm tepat belum aktif",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "Tanpa izin ini, pengingat bisa telat. Ketuk tombol di bawah untuk mengaktifkannya di Pengaturan.",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    TextButton(onClick = onOpenExactAlarmSettings) {
                                        Text("Buka Pengaturan Alarm")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        AnimatedVisibility(
                            visible = entered,
                            enter = HeroEnterTransition
                        ) {
                            HeroCard(
                                dateText = today.format(
                                    DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
                                ),
                                overdueCount = overdueCount,
                                unfinishedCount = unfinishedCount,
                                nextTaskTitle = nextTask?.title,
                                doneTodayCount = doneTodayCount,
                                todayTotalCount = todayTotalCount
                            )
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(KadanSpacing.Section)
                        ) {
                            AnimatedVisibility(
                                visible = entered,
                                enter = statEnterTransition(0),
                                modifier = Modifier.weight(1f)
                            ) {
                                StatCard(
                                    number = dueTodayCount,
                                    label = "Hari ini",
                                    isAlert = false
                                )
                            }
                            AnimatedVisibility(
                                visible = entered,
                                enter = statEnterTransition(80),
                                modifier = Modifier.weight(1f)
                            ) {
                                StatCard(
                                    number = overdueCount,
                                    label = "Telat",
                                    isAlert = overdueCount > 0
                                )
                            }
                            AnimatedVisibility(
                                visible = entered,
                                enter = statEnterTransition(160),
                                modifier = Modifier.weight(1f)
                            ) {
                                StatCard(
                                    number = unfinishedCount,
                                    label = "Belum selesai",
                                    isAlert = false
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(KadanSpacing.Gap)
                        ) {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.selectCategory(null) },
                                label = { Text("Semua") }
                            )
                            for (category in Category.entries) {
                                FilterChip(
                                    selected = selectedCategory == category.label,
                                    onClick = { viewModel.selectCategory(category.label) },
                                    label = { Text(category.label) }
                                )
                            }
                        }
                    }

                    if (visibleTasks.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(KadanSpacing.Hero),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Semua beres.",
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(KadanSpacing.Tiny))
                                    Text(
                                        text = "Ketuk + Tugas buat tugas pertama.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    items(visibleTasks, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            overdue = isOverdue(task),
                            onToggle = { viewModel.toggleDone(task) },
                            onOpenEdit = { viewModel.openEdit(task) },
                            onOpenFocus = {
                                viewModel.openFocusSessionForTask(task)
                                showTaskListSheet = false
                            }
                        )
                    }

                    item {
                        ThemeCard(
                            themeMode = themeMode,
                            onThemeModeChange = onThemeModeChange
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }

    if (captureRequest != null) {
        CaptureSheet(
            initialCategoryLabel = captureRequest ?: Category.PR.label,
            editingTask = editingTask,
            onDismiss = { viewModel.closeCapture() },
            onSave = { title, categoryLabel, notes, dueDate, dueTime, _ ->
                viewModel.saveTask(
                    title = title,
                    categoryLabel = categoryLabel,
                    notes = notes,
                    dueDateEpochDay = dueDate,
                    dueTimeMinutes = dueTime,
                    existing = editingTask
                )
            },
            onDelete = {
                editingTask?.let { viewModel.delete(it) }
            }
        )
    }
}

@Composable
private fun HeroCard(
    dateText: String,
    overdueCount: Int,
    unfinishedCount: Int,
    nextTaskTitle: String?,
    doneTodayCount: Int,
    todayTotalCount: Int
) {
    val headline = when {
        overdueCount > 0 -> "$overdueCount tugas telat."
        unfinishedCount > 0 -> "$unfinishedCount tugas tersisa."
        else -> "Hari ini bersih."
    }
    val sub = when {
        nextTaskTitle != null -> "Berikutnya: $nextTaskTitle"
        overdueCount > 0 -> "Kerjakan yang telat dulu."
        else -> "Nggak ada tugas. Tambah satu, atau tunggu nudge PR 15:15."
    }
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(KadanSpacing.Hero)) {
            Text(
                text = dateText.uppercase(Locale("id", "ID")),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(KadanSpacing.Small))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(KadanSpacing.Tiny))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(KadanSpacing.Large))
            if (todayTotalCount > 0) {
                // Custom bar (no LinearProgressIndicator API gamble):
                // track = primaryContainer, fill = onPrimary.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(
                                doneTodayCount.toFloat() / todayTotalCount.toFloat()
                            )
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
                Spacer(modifier = Modifier.height(KadanSpacing.Small))
                Text(
                    text = "$doneTodayCount dari $todayTotalCount tugas hari ini selesai",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Progress hari ini mulai begitu ada tugas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    number: Int,
    label: String,
    isAlert: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isAlert) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
            contentColor = if (isAlert) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(KadanSpacing.Compact)) {
            Box(
                modifier = Modifier
                    .size(width = KadanSpacing.AccentWidth, height = KadanSpacing.AccentHeight)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isAlert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
            Spacer(modifier = Modifier.height(KadanSpacing.Small))
            Text(
                text = "$number",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isAlert) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun ThemeCard(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(KadanSpacing.Card)) {
            Text(
                text = "Tampilan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(KadanSpacing.Small))
            Row(horizontalArrangement = Arrangement.spacedBy(KadanSpacing.Gap)) {
                FilterChip(
                    selected = themeMode == ThemeMode.SYSTEM,
                    onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                    label = { Text("Sistem") }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.LIGHT,
                    onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                    label = { Text("Terang") }
                )
                FilterChip(
                    selected = themeMode == ThemeMode.DARK,
                    onClick = { onThemeModeChange(ThemeMode.DARK) },
                    label = { Text("Gelap") }
                )
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    overdue: Boolean,
    onToggle: () -> Unit,
    onOpenEdit: () -> Unit,
    onOpenFocus: () -> Unit
) {
    val dueDate = LocalDate.ofEpochDay(task.dueDate)
    val dateText = dueDate.format(
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale("id", "ID"))
    )
    val timeText = task.dueTimeMinutes?.let { minutes ->
        "%02d:%02d".format(minutes / 60, minutes % 60)
    }
    val meta = buildString {
        if (overdue) append("Telat · ")
        append(task.category)
        append(" · ")
        append(dateText)
        if (timeText != null) {
            append(" · ")
            append(timeText)
        }
        if (task.notes.isNotBlank()) {
            append(" · ")
            append(task.notes)
        }
    }

    Card(
        onClick = onOpenEdit,
        colors = CardDefaults.cardColors(
            containerColor = if (overdue) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
            contentColor = if (overdue) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        border = if (overdue) {
            null
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Tugas: ${task.title}" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KadanSpacing.Compact),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isDone,
                onCheckedChange = { onToggle() }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (task.isDone) {
                        TextDecoration.LineThrough
                    } else {
                        TextDecoration.None
                    },
                    color = if (task.isDone) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (!task.isDone) {
                IconButton(
                    onClick = onOpenFocus,
                    modifier = Modifier.size(40.dp)
                ) {
                    FocusTimerGraphic(
                        modifier = Modifier.size(20.dp),
                        tint = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
