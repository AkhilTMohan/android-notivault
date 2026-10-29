package com.bc.notivault.ui.screens.appnotifications

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.ui.screens.dashboard.DashboardFilter
import com.bc.notivault.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppNotificationsUiState(
    val packageName: String = "",
    val appName: String = "",
    val notifications: List<NotificationRecordEntity> = emptyList(),
    val groupedNotifications: Map<String, List<NotificationRecordEntity>> = emptyMap(),
    val searchQuery: String = "",
    val currentFilter: DashboardFilter = DashboardFilter.ALL,
    val selectedDate: Long? = null,
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val isExcluded: Boolean = false
)

@HiltViewModel
class AppNotificationsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val packageName: String = checkNotNull(savedStateHandle["packageName"])
    private val initialDateParam: String? = savedStateHandle["date"]

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(DashboardFilter.ALL)
    val currentFilter: StateFlow<DashboardFilter> = _currentFilter.asStateFlow()

    private val _selectedDate = MutableStateFlow<Long?>(
        initialDateParam?.toLongOrNull()?.let { DateTimeUtils.getStartOfDay(it) }
    )
    val selectedDate: StateFlow<Long?> = _selectedDate.asStateFlow()

    val appName: String = try {
        val pm = context.packageManager
        val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(packageName, 0)
        }
        pm.getApplicationLabel(appInfo).toString()
    } catch (_: Exception) {
        packageName
    }

    private val startOfToday = DateTimeUtils.getStartOfDay()

    val uiState: StateFlow<AppNotificationsUiState> = combine(
        repository.getNotificationsByPackage(packageName),
        _searchQuery,
        _currentFilter,
        _selectedDate,
        preferenceManager.excludedPackages
    ) { notifications, query, filter, date, excluded ->
        val appTodayCount = notifications.count { it.captureTime >= startOfToday }

        val filtered = notifications.filter { item ->
            // Date filter
            if (date != null) {
                val dayStart = DateTimeUtils.getStartOfDay(date)
                val dayEnd = DateTimeUtils.getEndOfDay(date)
                if (item.captureTime < dayStart || item.captureTime > dayEnd) {
                    return@filter false
                }
            }

            // Status filter
            val matchesFilter = when (filter) {
                DashboardFilter.ALL -> true
                DashboardFilter.TODAY -> item.captureTime >= startOfToday
                DashboardFilter.STARRED -> item.isStarred
                DashboardFilter.ONGOING -> item.isOngoing && item.removalTime == null
                DashboardFilter.DISMISSED -> item.interactionStatus == InteractionStatus.DISMISSED
            }
            if (!matchesFilter) return@filter false

            // Query filter
            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val titleMatch = item.title?.lowercase()?.contains(q) == true
                val textMatch = item.text?.lowercase()?.contains(q) == true
                val bigTextMatch = item.bigText?.lowercase()?.contains(q) == true
                titleMatch || textMatch || bigTextMatch
            } else {
                true
            }
        }

        val grouped = filtered.groupBy { DateTimeUtils.getDateGroupHeader(it.postTime) }

        AppNotificationsUiState(
            packageName = packageName,
            appName = appName,
            notifications = filtered,
            groupedNotifications = grouped,
            searchQuery = query,
            currentFilter = filter,
            selectedDate = date,
            totalCount = notifications.size,
            todayCount = appTodayCount,
            isExcluded = excluded.contains(packageName)
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppNotificationsUiState(packageName = packageName, appName = appName)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: DashboardFilter) {
        _currentFilter.value = filter
    }

    fun onDateSelected(date: Long?) {
        _selectedDate.value = date?.let { DateTimeUtils.getStartOfDay(it) }
    }

    fun toggleStar(id: Long, isStarred: Boolean) {
        viewModelScope.launch {
            repository.toggleStar(id, isStarred)
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun toggleAppExclusion(exclude: Boolean) {
        viewModelScope.launch {
            preferenceManager.toggleExcludedPackage(packageName, exclude)
        }
    }
}
