package com.azerostudio.kadan.ui

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.azerostudio.kadan.data.Category
import com.azerostudio.kadan.data.Task
import com.azerostudio.kadan.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Home screen — "Hari Ini", v1.3 redesign.
 *
 * v1.3 is a VISUAL break from v1.2, not another token tweak: a solid
 * ocean hero card (headline status + today progress) replaces the grey
 * summary strip, the three counts become three separate stat cards,
 * the FAB is solid ocean, and the theme picker moves out of the top
 * of the screen into a footer card. Nothing about reminders, data or
 * capture changes — this file only re-presents the same counts.
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

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                ),
                title = {
                    Column {
                        Text(
                            text = "KADAN",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Hari Ini",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCapture(Category.PR.label) },
                text = { Text("Tambah") },
                icon = { Text("+") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Exact-alarm banner: only when the system has not granted it.
            // Without this, due reminders silently fall back to inexact
            // alarms — the user must be told, not left to discover it.
            if (!exactAlarmGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
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

            // v1.3 hero: solid ocean, one status headline, today's
            // progress bar. This is the first thing on screen, so even
            // with zero tasks the screen cannot look like v1.2's grey
            // strip anymore. Overdue is still words + colour, never
            // colour alone, and the next task is named, not counted.
            item {
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

            // v1.3 stat cards: three separate cards, not one strip.
            // Telat turns errorContainer only when it is above zero.
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        number = dueTodayCount,
                        label = "Hari ini",
                        isAlert = false,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        number = overdueCount,
                        label = "Telat",
                        isAlert = overdueCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        number = unfinishedCount,
                        label = "Belum selesai",
                        isAlert = false,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter chips: All + the 5 locked categories.
            item {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Semua beres.",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Ketuk Tambah buat tugas pertama, atau tunggu nudge PR jam 15:15.",
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
                    onOpenEdit = { viewModel.openEdit(task) }
                )
            }

            // v1.3: theme picker moved to the footer, so the top of the
            // screen is the hero + stats, not settings. Same DataStore
            // persistence via MainActivity, same three modes.
            item {
                ThemeCard(
                    themeMode = themeMode,
                    onThemeModeChange = onThemeModeChange
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
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
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = dateText.uppercase(Locale("id", "ID")),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = sub,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(16.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
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
    isAlert: Boolean,
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
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Box(
                modifier = Modifier
                    .size(width = 28.dp, height = 4.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(
                        if (isAlert) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
            )
            Spacer(modifier = Modifier.height(8.dp))
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
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tampilan",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    onOpenEdit: () -> Unit
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
                .padding(12.dp),
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
        }
    }
}
