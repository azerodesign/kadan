package com.azerostudio.kadan.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * One Kadan task. Offline-first, local only — no login, no sync, no cloud.
 *
 *  dueDate: epoch day (LocalDate.toEpochDay), e.g. the day a PR is due.
 *  dueTimeMinutes: minutes since midnight in Asia/Jakarta local time, or
 *    null when the user only set a date. Null time = "due that day", and
 *    the due reminder fires in the morning of that day (see
 *    ReminderScheduler) instead of at a specific minute.
 *  isRecurring: v1 stores the flag for Rutinitas, but v1 does NOT yet
 *    auto-roll a recurring task to the next day. That is a deliberate v1
 *    cut (see README): a recurring task behaves like a normal task in v1,
 *    and the user re-creates it or edits its date. Do not claim otherwise.
 *  createdAt: epoch millis, for stable ordering (newest last in lists
 *    only as a tie-breaker; due date/time is the primary sort).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: String,
    val notes: String = "",
    val dueDate: Long,
    val dueTimeMinutes: Int? = null,
    val isDone: Boolean = false,
    val isRecurring: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
