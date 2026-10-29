package com.azerostudio.kadan.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.azerostudio.kadan.data.Category
import com.azerostudio.kadan.data.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/*
 * Capture sheet — the reminder-first flow.
 * Target: title + category + date completable in under ~10 seconds.
 * That is why the sheet opens with a category already selected (PR by
 * default, or the category passed by the PR nudge), the date defaults to
 * today, and time is optional — a user who only sets a date still gets a
 * morning reminder for that day (see ReminderScheduler).
 *
 * Material 3 notes:
 *  - ModalBottomSheet (not a persistent sheet, not a dialog) for capture.
 *  - DatePickerDialog exists in M3 Compose. TimePickerDialog does NOT
 *    exist in the M3 Compose API — the TimePicker is wrapped in an
 *    AlertDialog here, which is the documented workaround.
 *  - Save = filled Button (high emphasis, final action).
 *    Cancel = TextButton (low emphasis). Delete (edit mode only) =
 *    TextButton in the error role, with confirmation handled by the
 *    caller's Snackbar Undo — deleting from the sheet is recoverable,
 *    so no extra confirmation dialog is stacked on top.
 *  - DatePicker selectedDateMillis is UTC milliseconds. Converting it
 *    with epochDay = millis / 86_400_000 is the standard way to get the
 *    calendar day the user actually picked, without a timezone shift.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    initialCategoryLabel: String,
    editingTask: Task?,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        categoryLabel: String,
        notes: String,
        dueDateEpochDay: Long,
        dueTimeMinutes: Int?,
        isRecurring: Boolean
    ) -> Unit,
    onDelete: () -> Unit
) {
    val zone = KadanViewModel.ZONE
    var title by remember { mutableStateOf(editingTask?.title ?: "") }
    var notes by remember { mutableStateOf(editingTask?.notes ?: "") }
    var categoryLabel by remember { mutableStateOf(initialCategoryLabel) }
    var dueDateEpochDay by remember {
        mutableStateOf(editingTask?.dueDate ?: LocalDate.now(zone).toEpochDay())
    }
    var dueTimeMinutes by remember { mutableIntStateOf(editingTask?.dueTimeMinutes ?: -1) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingTask != null) "Ubah tugas" else "Tugas baru",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Apa yang harus dilakukan?") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (category in Category.entries) {
                    FilterChip(
                        selected = categoryLabel == category.label,
                        onClick = { categoryLabel = category.label },
                        label = { Text(category.label) }
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { showDatePicker = true }) {
                    Text(
                        LocalDate.ofEpochDay(dueDateEpochDay).format(
                            DateTimeFormatter.ofPattern("EEEE, d MMM yyyy", Locale("id", "ID"))
                        )
                    )
                }
                if (dueTimeMinutes >= 0) {
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("%02d:%02d".format(dueTimeMinutes / 60, dueTimeMinutes % 60))
                    }
                    TextButton(onClick = { dueTimeMinutes = -1 }) {
                        Text("Hapus jam")
                    }
                } else {
                    TextButton(onClick = { showTimePicker = true }) {
                        Text("Atur jam (opsional)")
                    }
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Catatan (opsional)") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (editingTask != null) {
                    TextButton(
                        onClick = onDelete
                    ) {
                        Text(
                            "Hapus",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    TextButton(onClick = onDismiss) {
                        Text("Batal")
                    }
                }
                Button(
                    enabled = title.isNotBlank(),
                    onClick = {
                        onSave(
                            title.trim(),
                            categoryLabel,
                            notes.trim(),
                            dueDateEpochDay,
                            if (dueTimeMinutes >= 0) dueTimeMinutes else null,
                            categoryLabel == Category.RUTINITAS.label
                        )
                    }
                ) {
                    Text("Simpan")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDateEpochDay * 86_400_000L
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDateEpochDay = millis / 86_400_000L
                        }
                        showDatePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
            }
        ) {
            DatePicker(state = datePickerState, showModeToggle = false)
        }
    }

    if (showTimePicker) {
        val initialHour = if (dueTimeMinutes >= 0) dueTimeMinutes / 60 else 15
        val initialMinute = if (dueTimeMinutes >= 0) dueTimeMinutes % 60 else 15
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Pilih jam") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        dueTimeMinutes = timePickerState.hour * 60 + timePickerState.minute
                        showTimePicker = false
                    }
                ) { Text("Pilih") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Batal") }
            }
        )
    }
}
