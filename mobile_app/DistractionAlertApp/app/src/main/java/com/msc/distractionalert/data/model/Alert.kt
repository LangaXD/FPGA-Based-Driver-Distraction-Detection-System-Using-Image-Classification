package com.msc.distractionalert.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single distraction alert, either fetched from GET /api/alerts or received
 * as a push notification and cached locally so it shows up in history right away.
 *
 * [id] mirrors the server's alert id when known. Locally created rows (from FCM,
 * before the next server refresh) use a negative id derived from the timestamp so
 * they never collide with real server ids, which are always positive.
 */
@Entity(tableName = "alerts")
data class Alert(
    @PrimaryKey val id: Long,
    val timestamp: String,
    val className: String,
    val confidence: Float,
    // Locally-cached rows from FCM (negative id, before the next server
    // refresh) default this to false rather than guessing - the real value
    // and the image itself only exist once the server-assigned positive id
    // is known, which the following /api/alerts refresh provides.
    val hasImage: Boolean = false
)
