package com.msc.distractionalert.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.msc.distractionalert.data.model.Alert

@Database(entities = [Alert::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alertDao(): AlertDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "distraction_alerts.db"
                )
                    // This table is a pure cache of the server's /api/alerts response
                    // (AlertRepository.refresh() always clears and repopulates it), never
                    // user-entered data, so there is nothing a real migration would need
                    // to preserve - dropping and recreating on a schema bump (like adding
                    // hasImage) is the correct choice here, not a shortcut.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
