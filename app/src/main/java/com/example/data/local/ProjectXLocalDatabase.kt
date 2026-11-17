package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        LocalTransactionEntity::class,
        LocalKpiSnapshotEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ProjectXLocalDatabase : RoomDatabase() {
    abstract fun businessOverviewDao(): BusinessOverviewDao

    companion object {
        @Volatile
        private var INSTANCE: ProjectXLocalDatabase? = null

        fun getInstance(context: Context): ProjectXLocalDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ProjectXLocalDatabase::class.java,
                    "project_x_local_overview.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
