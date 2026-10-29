package com.azerostudio.kadan.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.azerostudio.kadan.data.Category
import com.azerostudio.kadan.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Home screen — "Hari Ini".
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
    exactAlarmGranted: Boolean
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
                title = {
                    Column {
                        Text(
                            text = "Hari Ini",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = today.format(
                                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.openCapture(Category.PR.label) },
                text = { Text("Tambah") },
                icon = { Text("+") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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

            // Counts header: due today + overdue, in words as well as colour.
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "$dueTodayCount jatuh tempo hari ini",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (overdueCount > 0) {
                                    "$overdueCount telat — kerjakan dulu yang ini"
                                } else {
                                    "Tidak ada yang telat"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (overdueCount > 0) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Text(
                            text = "${tasks.count { !it.isDone }} belum selesai",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Belum ada tugas",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Ketuk Tambah, atau tunggu nudge PR jam 15:15.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
                MaterialTheme.colorScheme.surfaceContainerLow
            },
            contentColor = if (overdue) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Tugas: ${task.title}" }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
