package com.msc.distractionalert.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * The server base URL is user-editable on the Settings screen, so this can't be a
 * single static Retrofit instance built once at app startup. Instead it lazily
 * rebuilds and caches the client, keyed by base URL, so switching servers mid-session
 * (e.g. pointing at a different FastAPI VM) picks up the new address immediately.
 */
object RetrofitProvider {

    private var cachedBaseUrl: String? = null
    private var cachedService: ApiService? = null

    fun getApiService(baseUrl: String): ApiService {
        val normalizedUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        cachedService?.let {
            if (cachedBaseUrl == normalizedUrl) return it
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val service = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)

        cachedBaseUrl = normalizedUrl
        cachedService = service
        return service
    }
}
