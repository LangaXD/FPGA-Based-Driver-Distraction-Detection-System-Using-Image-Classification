package com.msc.distractionalert.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.msc.distractionalert.data.local.PrefsManager
import com.msc.distractionalert.data.repository.AuthRepository

class SettingsViewModel(
    private val prefsManager: PrefsManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    val serverUrl: String get() = prefsManager.serverUrl
    val notificationsEnabled: Boolean get() = prefsManager.notificationsEnabled

    fun saveServerUrl(url: String) {
        prefsManager.serverUrl = url
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefsManager.notificationsEnabled = enabled
    }

    fun logout() {
        authRepository.logout()
    }

    class Factory(
        private val prefsManager: PrefsManager,
        private val authRepository: AuthRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SettingsViewModel(prefsManager, authRepository) as T
    }
}
