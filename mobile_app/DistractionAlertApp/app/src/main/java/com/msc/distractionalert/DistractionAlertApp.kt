package com.msc.distractionalert

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.msc.distractionalert.data.local.AppDatabase
import com.msc.distractionalert.data.local.PrefsManager
import com.msc.distractionalert.data.repository.AlertRepository
import com.msc.distractionalert.data.repository.AuthRepository

/**
 * No dependency-injection framework here on purpose: the object graph is tiny
 * (two repositories, one database, one prefs store), so a Hilt/Koin setup would
 * add more ceremony than it removes. Screens read `(application as DistractionAlertApp)`
 * to get at these singletons.
 */
class DistractionAlertApp : Application() {

    lateinit var prefsManager: PrefsManager
        private set
    lateinit var authRepository: AuthRepository
        private set
    lateinit var alertRepository: AlertRepository
        private set

    override fun onCreate() {
        super.onCreate()

        prefsManager = PrefsManager(this)
        val alertDao = AppDatabase.getInstance(this).alertDao()

        authRepository = AuthRepository(prefsManager)
        alertRepository = AlertRepository(alertDao, prefsManager)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
