package com.msc.distractionalert.data.repository

import com.msc.distractionalert.data.local.PrefsManager
import com.msc.distractionalert.data.model.FcmTokenRequest
import com.msc.distractionalert.data.model.LoginRequest
import com.msc.distractionalert.data.remote.RetrofitProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Result of a login attempt, distinguishing bad credentials from network/server failures. */
sealed class LoginResult {
    object Success : LoginResult()
    object InvalidCredentials : LoginResult()
    data class Error(val message: String) : LoginResult()
}

class AuthRepository(private val prefsManager: PrefsManager) {

    val isLoggedIn: Boolean get() = prefsManager.isLoggedIn

    suspend fun login(username: String, password: String): LoginResult = withContext(Dispatchers.IO) {
        try {
            val api = RetrofitProvider.getApiService(prefsManager.serverUrl)
            val response = api.login(LoginRequest(username, password))

            when {
                response.isSuccessful && response.body() != null -> {
                    prefsManager.authToken = response.body()!!.token
                    LoginResult.Success
                }
                response.code() == 401 -> LoginResult.InvalidCredentials
                else -> LoginResult.Error("Server returned ${response.code()}")
            }
        } catch (e: Exception) {
            LoginResult.Error(e.message ?: "Network error")
        }
    }

    fun logout() {
        prefsManager.clearSession()
    }

    /** Sends the current FCM registration token to the backend, if the user is logged in. */
    suspend fun registerFcmToken(fcmToken: String) {
        val authToken = prefsManager.authToken ?: return
        withContext(Dispatchers.IO) {
            try {
                val api = RetrofitProvider.getApiService(prefsManager.serverUrl)
                api.sendFcmToken(
                    bearerToken = "Bearer $authToken",
                    request = FcmTokenRequest(fcmToken)
                )
            } catch (e: Exception) {
                // Best-effort: the token will be retried next time FCM refreshes it,
                // or next login. No user-facing action needed if this single call fails.
            }
        }
    }
}
