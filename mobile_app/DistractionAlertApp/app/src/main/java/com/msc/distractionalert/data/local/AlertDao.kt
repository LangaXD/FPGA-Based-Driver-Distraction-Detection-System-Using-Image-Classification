package com.msc.distractionalert.data.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.msc.distractionalert.data.model.Alert

@Dao
interface AlertDao {

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun observeAlerts(): LiveData<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: Alert)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(alerts: List<Alert>)

    @Query("DELETE FROM alerts")
    suspend fun clear()

    /**
     * Replaces the cache with a fresh server response in one transaction so the
     * list never flashes empty between a clear and the following insert.
     */
    @androidx.room.Transaction
    suspend fun replaceAll(alerts: List<Alert>) {
        clear()
        insertAll(alerts)
    }
}
