package com.msc.distractionalert.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.msc.distractionalert.data.model.Alert
import com.msc.distractionalert.data.repository.AlertRepository
import kotlinx.coroutines.launch

/**
 * Shared by HistoryFragment and ChartFragment (via activityViewModels()) so both
 * tabs render the same alert list and a single refresh updates them together.
 */
class AlertsViewModel(private val alertRepository: AlertRepository) : ViewModel() {

    val alerts: LiveData<List<Alert>> = alertRepository.observeAlerts()

    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    private val _refreshError = MutableLiveData<String?>(null)
    val refreshError: LiveData<String?> = _refreshError

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            alertRepository.refresh()
                .onFailure { _refreshError.value = it.message ?: "Unknown error" }
            _isRefreshing.value = false
        }
    }

    class Factory(private val alertRepository: AlertRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            AlertsViewModel(alertRepository) as T
    }
}
