package com.azerostudio.kadan.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTimeMinutes ASC, createdAt ASC")
    fun observeAll(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, dueTimeMinutes ASC, createdAt ASC")
    suspend fun getAll(): List<Task>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Task?

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
