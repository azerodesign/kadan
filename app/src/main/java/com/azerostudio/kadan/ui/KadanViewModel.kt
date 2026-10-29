package com.azerostudio.kadan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.azerostudio.kadan.data.Category
import com.azerostudio.kadan.data.Task
import com.azerostudio.kadan.data.TaskRepository
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/*
 * KadanViewModel — the only place HomeScreen gets data from.
 * Scheduling is NOT done here in the abstract: MainActivity / the caller
 * passes a scheduler lambda so the ViewModel stays testable without an
 * Android Context, while the real app wires it to ReminderScheduler.
 * In practice MainActivity constructs this ViewModel with
 * onTaskSaved = { task -> ReminderScheduler.scheduleDue(context, task) }
 * and onTaskRemoved = { id -> ReminderScheduler.cancelDue(context, id) }.
 */
class KadanViewModel(
    private val repository: TaskRepository,
    private val onTaskSaved: (Task) -> Unit = {},
    private val onTaskRemoved: (Long) -> Unit = {}
) : ViewModel() {

    val tasks: StateFlow<List<Task>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Non-null = capture sheet is open, with this category pre-selected.
    private val _captureRequest = MutableStateFlow<String?>(null)
    val captureRequest: StateFlow<String?> = _captureRequest.asStateFlow()

    // Non-null = the task being edited (null = creating a new task).
    private val _editingTask = MutableStateFlow<Task?>(null)
    val editingTask: StateFlow<Task?> = _editingTask.asStateFlow()

    private val _pendingDeleted = MutableStateFlow<Task?>(null)
    val pendingDeleted: StateFlow<Task?> = _pendingDeleted.asStateFlow()

    fun selectCategory(label: String?) {
        _selectedCategory.value = label
    }

    fun openCapture(categoryLabel: String = Category.PR.label) {
        _editingTask.value = null
        _captureRequest.value = categoryLabel
    }

    fun openEdit(task: Task) {
        _editingTask.value = task
        _captureRequest.value = task.category
    }

    fun closeCapture() {
        _captureRequest.value = null
        _editingTask.value = null
    }

    fun saveTask(
        title: String,
        categoryLabel: String,
        notes: String,
        dueDateEpochDay: Long,
        dueTimeMinutes: Int?,
        existing: Task? = null
    ) {
        viewModelScope.launch {
            val task = if (existing != null) {
                existing.copy(
                    title = title,
                    category = categoryLabel,
                    notes = notes,
                    dueDate = dueDateEpochDay,
                    dueTimeMinutes = dueTimeMinutes,
                    isRecurring = categoryLabel == Category.RUTINITAS.label
                )
            } else {
                Task(
                    title = title,
                    category = categoryLabel,
                    notes = notes,
                    dueDate = dueDateEpochDay,
                    dueTimeMinutes = dueTimeMinutes,
                    isRecurring = categoryLabel == Category.RUTINITAS.label
                )
            }
            val id = if (existing != null) {
                repository.update(task)
                task.id
            } else {
                repository.add(task)
            }
            val saved = task.copy(id = id)
            onTaskSaved(saved)
            closeCapture()
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val updated = task.copy(isDone = !task.isDone)
            repository.update(updated)
            if (updated.isDone) {
                onTaskRemoved(task.id)
            } else {
                onTaskSaved(updated)
            }
        }
    }

    fun delete(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
            onTaskRemoved(task.id)
            _pendingDeleted.value = task
            closeCapture()
        }
    }

    fun restoreDeleted() {
        val task = _pendingDeleted.value ?: return
        viewModelScope.launch {
            // Re-insert as a new row (new id) so its alarm can be scheduled
            // cleanly; the old id's alarm was already cancelled on delete.
            val restored = task.copy(id = 0)
            val newId = repository.add(restored)
            onTaskSaved(restored.copy(id = newId))
            _pendingDeleted.value = null
        }
    }

    fun clearPendingDeleted() {
        _pendingDeleted.value = null
    }

    companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
        fun todayEpochDay(): Long = LocalDate.now(ZONE).toEpochDay()
    }
}
