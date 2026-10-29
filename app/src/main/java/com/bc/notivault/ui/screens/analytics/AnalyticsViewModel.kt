package com.bc.notivault.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.model.AppStatInfo
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class StatusStat(
    val status: InteractionStatus,
    val count: Int,
    val percentage: Float
)

data class AnalyticsUiState(
    val totalCount: Int = 0,
    val todayCount: Int = 0,
    val distinctAppsCount: Int = 0,
    val topApps: List<AppStatInfo> = emptyList(),
    val statusStats: List<StatusStat> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val startOfDay = DateTimeUtils.getStartOfDay()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getTotalCount(),
        repository.getTodayCount(startOfDay),
        repository.getAppStats(),
        repository.getAllNotifications()
    ) { total, today, appStats, allList ->
        val statusCounts = allList.groupingBy { it.interactionStatus }.eachCount()
        val totalValid = if (total > 0) total.toFloat() else 1f

        val statusStats = InteractionStatus.values().mapNotNull { status ->
            val count = statusCounts[status] ?: 0
            if (count > 0) {
                StatusStat(
                    status = status,
                    count = count,
                    percentage = (count / totalValid) * 100f
                )
            } else null
        }.sortedByDescending { it.count }

        AnalyticsUiState(
            totalCount = total,
            todayCount = today,
            distinctAppsCount = appStats.size,
            topApps = appStats.take(10),
            statusStats = statusStats
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )
}
