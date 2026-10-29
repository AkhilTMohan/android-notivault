package com.bc.notivault.ui.screens.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.service.NotiVaultListenerService
import com.bc.notivault.util.DateTimeUtils
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

enum class DashboardFilter(val label: String) {
    ALL("All"),
    TODAY("Today"),
    STARRED("Starred"),
    ONGOING("Ongoing"),
    DISMISSED("Dismissed")
}

data class AppVaultCard(
    val packageName: String,
    val appName: String,
    val totalCount: Int,
    val todayCount: Int,
    val latestTitle: String?,
    val latestSnippet: String?,
    val latestTimestamp: Long,
    val latestStatus: InteractionStatus,
    val hasOngoing: Boolean,
    val hasStarred: Boolean
)

data class DashboardUiState(
    val recentApps: List<AppVaultCard> = emptyList(),
    val latestNotifications: List<NotificationRecordEntity> = emptyList(),
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val todayAppCount: Int = 0,
    val ongoingCount: Int = 0,
    val searchQuery: String = "",
    val currentFilter: DashboardFilter = DashboardFilter.ALL,
    val isPermissionGranted: Boolean = false,
    val isServiceConnected: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(DashboardFilter.ALL)
    val currentFilter: StateFlow<DashboardFilter> = _currentFilter.asStateFlow()

    private val _isPermissionGranted = MutableStateFlow(false)
    val isPermissionGranted: StateFlow<Boolean> = _isPermissionGranted.asStateFlow()

    val isServiceConnected: StateFlow<Boolean> = NotiVaultListenerService.isServiceConnected

    private val startOfDay = DateTimeUtils.getStartOfDay()

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllNotifications(),
        _searchQuery,
        _currentFilter,
        _isPermissionGranted,
        isServiceConnected,
        repository.getTotalCount(),
        repository.getTodayCount(startOfDay),
        repository.getActiveOngoingCount()
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val allList = params[0] as List<NotificationRecordEntity>
        val query = params[1] as String
        val filter = params[2] as DashboardFilter
        val permGranted = params[3] as Boolean
        val serviceConn = params[4] as Boolean
        val totalCount = params[5] as Int
        val todayCount = params[6] as Int
        val ongoingCount = params[7] as Int

        val todayNotifications = allList.filter { it.captureTime >= startOfDay }
        val todayAppCount = todayNotifications.map { it.packageName }.distinct().size

        // Build one card per app for Recent Apps
        val groupedByPackage = allList.groupBy { it.packageName }
        val appCards = groupedByPackage.map { (pkg, notifications) ->
            val sorted = notifications.sortedByDescending { it.captureTime }
            val latest = sorted.first()
            val appTodayCount = notifications.count { it.captureTime >= startOfDay }
            val hasOngoing = notifications.any { it.isOngoing && it.removalTime == null }
            val hasStarred = notifications.any { it.isStarred }

            AppVaultCard(
                packageName = pkg,
                appName = latest.appName,
                totalCount = notifications.size,
                todayCount = appTodayCount,
                latestTitle = latest.title,
                latestSnippet = latest.bigText ?: latest.text ?: latest.subText,
                latestTimestamp = latest.postTime,
                latestStatus = latest.interactionStatus,
                hasOngoing = hasOngoing,
                hasStarred = hasStarred
            )
        }.sortedByDescending { it.latestTimestamp }

        // Filter notifications based on search and selected filter chip
        val filteredNotifications = allList.filter { item ->
            val matchesFilter = when (filter) {
                DashboardFilter.ALL -> true
                DashboardFilter.TODAY -> item.captureTime >= startOfDay
                DashboardFilter.STARRED -> item.isStarred
                DashboardFilter.ONGOING -> item.isOngoing && item.removalTime == null
                DashboardFilter.DISMISSED -> item.interactionStatus == InteractionStatus.DISMISSED
            }
            if (!matchesFilter) return@filter false

            if (query.isNotBlank()) {
                val q = query.trim().lowercase()
                val titleMatch = item.title?.lowercase()?.contains(q) == true
                val textMatch = item.text?.lowercase()?.contains(q) == true
                val bigTextMatch = item.bigText?.lowercase()?.contains(q) == true
                val appMatch = item.appName.lowercase().contains(q)
                titleMatch || textMatch || bigTextMatch || appMatch
            } else {
                true
            }
        }

        DashboardUiState(
            recentApps = appCards,
            latestNotifications = filteredNotifications,
            totalCount = totalCount,
            todayCount = todayCount,
            todayAppCount = todayAppCount,
            ongoingCount = ongoingCount,
            searchQuery = query,
            currentFilter = filter,
            isPermissionGranted = permGranted,
            isServiceConnected = serviceConn
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun refreshPermission(context: Context) {
        _isPermissionGranted.value = NotificationPermissionHelper.isNotificationAccessGranted(context)
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onFilterChange(filter: DashboardFilter) {
        _currentFilter.value = filter
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

    fun excludeApp(packageName: String) {
        viewModelScope.launch {
            preferenceManager.toggleExcludedPackage(packageName, exclude = true)
        }
    }
}
