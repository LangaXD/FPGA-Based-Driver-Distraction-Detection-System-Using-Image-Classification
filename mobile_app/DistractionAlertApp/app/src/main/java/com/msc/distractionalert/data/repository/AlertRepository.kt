package com.msc.distractionalert.data.repository

import androidx.lifecycle.LiveData
import com.msc.distractionalert.data.local.AlertDao
import com.msc.distractionalert.data.local.PrefsManager
import com.msc.distractionalert.data.model.Alert
import com.msc.distractionalert.data.remote.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlertRepository(
    private val alertDao: AlertDao,
    private val prefsManager: PrefsManager
) {

    /** Room is the single source of truth for the UI; refresh() just keeps it up to date. */
    fun observeAlerts(): LiveData<List<Alert>> = alertDao.observeAlerts()

    suspend fun refresh(): Result<Unit> = withContext(Dispatchers.IO) {
        val authToken = prefsManager.authToken
            ?: return@withContext Result.failure(IllegalStateException("Not logged in"))

        try {
            val api = RetrofitProvider.getApiService(prefsManager.serverUrl)
            val response = api.getAlerts("Bearer $authToken")

            if (response.isSuccessful && response.body() != null) {
                val alerts = response.body()!!.map { it.toAlert() }
                alertDao.replaceAll(alerts)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Server returned ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Called by the FCM service so a pushed alert appears in history immediately. */
    suspend fun cacheIncomingAlert(alert: Alert) = withContext(Dispatchers.IO) {
        alertDao.insert(alert)
    }
}
