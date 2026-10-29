package com.bc.notivault.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.util.NotificationPermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isPermissionGranted: Boolean = false,
    val retentionDays: Int = 30,
    val themeMode: String = "SYSTEM",
    val totalRecords: Int = 0,
    val excludedAppCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        _isPermissionGranted,
        preferenceManager.retentionDays,
        preferenceManager.themeMode,
        repository.getTotalCount(),
        preferenceManager.excludedPackages
    ) { perm, retention, theme, total, excluded ->
        SettingsUiState(
            isPermissionGranted = perm,
            retentionDays = retention,
            themeMode = theme,
            totalRecords = total,
            excludedAppCount = excluded.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun refreshPermission(context: Context) {
        _isPermissionGranted.value = NotificationPermissionHelper.isNotificationAccessGranted(context)
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            preferenceManager.setRetentionDays(days)
            if (days > 0) {
                repository.deleteOlderThanDays(days)
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferenceManager.setThemeMode(mode)
        }
    }

    fun clearAllHistory(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteAllNotifications()
            onComplete()
        }
    }

    suspend fun exportJson(): String = repository.exportNotificationsJson()

    suspend fun exportPlainText(): String = repository.exportNotificationsPlainText()

    suspend fun getExportJson(): String = repository.exportNotificationsJson()

    suspend fun getExportPlainText(): String = repository.exportNotificationsPlainText()
}
