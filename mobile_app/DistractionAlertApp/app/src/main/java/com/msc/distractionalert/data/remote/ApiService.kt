package com.msc.distractionalert.data.remote

import com.msc.distractionalert.data.model.AlertDto
import com.msc.distractionalert.data.model.FcmTokenRequest
import com.msc.distractionalert.data.model.LoginRequest
import com.msc.distractionalert.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/alerts")
    suspend fun getAlerts(@Header("Authorization") bearerToken: String): Response<List<AlertDto>>

    @POST("api/fcm-token")
    suspend fun sendFcmToken(
        @Header("Authorization") bearerToken: String,
        @Body request: FcmTokenRequest
    ): Response<Unit>
}
