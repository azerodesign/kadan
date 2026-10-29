package com.azerostudio.kadan.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class KadanDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: KadanDatabase? = null

        fun get(context: Context): KadanDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    KadanDatabase::class.java,
                    "kadan.db"
                ).build().also { instance = it }
            }
        }
    }
}
