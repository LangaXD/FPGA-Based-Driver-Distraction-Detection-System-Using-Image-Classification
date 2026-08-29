package com.msc.distractionalert.fcm

import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.msc.distractionalert.DistractionAlertApp
import com.msc.distractionalert.R
import com.msc.distractionalert.data.model.Alert
import com.msc.distractionalert.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Receives the board's distraction alerts, forwarded through the FastAPI backend as
 * FCM data messages (not notification messages) so onMessageReceived always runs -
 * even in the background - and we control the notification content and can cache
 * the alert locally before the next /api/alerts refresh.
 */
class DistractionFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = application as DistractionAlertApp
        serviceScope.launch {
            app.authRepository.registerFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val data = remoteMessage.data
        if (data.isEmpty()) return

        val title = data["title"] ?: getString(R.string.app_name)
        val body = data["body"] ?: ""
        val className = data["class_name"] ?: "unknown"
        val confidence = data["confidence"]?.toFloatOrNull() ?: 0f
        val timestamp = data["timestamp"] ?: java.time.Instant.now().toString()

        val app = application as DistractionAlertApp
        serviceScope.launch {
            app.alertRepository.cacheIncomingAlert(
                Alert(
                    id = -System.currentTimeMillis(), // negative: locally-sourced, never collides with a server id
                    timestamp = timestamp,
                    className = className,
                    confidence = confidence
                )
            )
        }

        if (app.prefsManager.notificationsEnabled) {
            showNotification(title, body)
        }
    }

    private fun showNotification(title: String, body: String) {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            android.content.Intent(this, MainActivity::class.java)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, getString(R.string.notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // MainActivity requests POST_NOTIFICATIONS (API 33+) at runtime on first launch;
        // if the user denied it, notify() is a safe no-op rather than a crash.
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
