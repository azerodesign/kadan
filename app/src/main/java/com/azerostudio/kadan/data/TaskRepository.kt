package com.azerostudio.kadan.data

import kotlinx.coroutines.flow.Flow

/*
 * Thin repository over TaskDao. It owns no scheduling: callers
 * (KadanViewModel) schedule/cancel alarms AFTER a successful database
 * write, so the database is never left claiming a reminder that was not
 * scheduled, and an alarm is never scheduled for a task that failed to save.
 */
class TaskRepository(private val dao: TaskDao) {
    fun observeAll(): Flow<List<Task>> = dao.observeAll()

    suspend fun getAll(): List<Task> = dao.getAll()

    suspend fun getById(id: Long): Task? = dao.getById(id)

    suspend fun add(task: Task): Long = dao.insert(task)

    suspend fun update(task: Task) = dao.update(task)

    suspend fun delete(task: Task) = dao.delete(task)
}
