package com.bc.notivault.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val notificationId: Long = checkNotNull(savedStateHandle["notificationId"])

    val notification: StateFlow<NotificationRecordEntity?> = repository.getNotificationById(notificationId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    fun toggleStar(isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(notificationId, isStarred)
        }
    }

    fun deleteNotification(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteNotification(notificationId)
            onDeleted()
        }
    }

    fun excludeApp(packageName: String) {
        viewModelScope.launch {
            preferenceManager.toggleExcludedPackage(packageName, exclude = true)
        }
    }
}
