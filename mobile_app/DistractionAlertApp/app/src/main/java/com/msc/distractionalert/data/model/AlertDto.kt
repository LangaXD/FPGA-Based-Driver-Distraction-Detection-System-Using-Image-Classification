package com.msc.distractionalert.data.model

import com.google.gson.annotations.SerializedName

/** Shape of a single element in the GET /api/alerts response body. */
data class AlertDto(
    val id: Long,
    val timestamp: String,
    @SerializedName("class_name") val className: String,
    val confidence: Float,
    @SerializedName("has_image") val hasImage: Boolean = false
) {
    fun toAlert() = Alert(
        id = id, timestamp = timestamp, className = className,
        confidence = confidence, hasImage = hasImage
    )
}

data class LoginRequest(val username: String, val password: String)

data class LoginResponse(val token: String)

data class FcmTokenRequest(@SerializedName("fcm_token") val fcmToken: String)
