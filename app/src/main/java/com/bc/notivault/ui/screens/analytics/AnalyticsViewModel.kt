package com.bc.notivault.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

enum class AnalyticsTimeRange(val label: String) {
    TODAY("Today"),
    LAST_7_DAYS("7 Days"),
    LAST_30_DAYS("30 Days"),
    ALL_TIME("All Time")
}

data class HourlyBucket(
    val hour: Int,
    val count: Int,
    val displayLabel: String
)

data class DayOfWeekBucket(
    val dayOfWeek: Int,
    val shortName: String,
    val count: Int,
    val percentage: Float
)

data class AppAnalyticsItem(
    val packageName: String,
    val appName: String,
    val notificationCount: Int,
    val percentage: Float,
    val dailyAverage: Float
)

data class StatusStat(
    val status: InteractionStatus,
    val count: Int,
    val percentage: Float
)

data class TimeSegmentSummary(
    val morningCount: Int = 0,    // 06:00 - 11:59
    val afternoonCount: Int = 0,  // 12:00 - 17:59
    val eveningCount: Int = 0,    // 18:00 - 22:59
    val nightCount: Int = 0       // 23:00 - 05:59
)

data class AnalyticsUiState(
    val timeRange: AnalyticsTimeRange = AnalyticsTimeRange.LAST_7_DAYS,
    val totalCount: Int = 0,
    val distinctAppsCount: Int = 0,
    val dailyAverage: Float = 0f,
    val peakHour: Int? = null,
    val peakHourCount: Int = 0,
    val quietHoursCount: Int = 0,
    val quietHoursPercentage: Float = 0f,
    val topNoisiestApp: AppAnalyticsItem? = null,
    val hourlyDistribution: List<HourlyBucket> = emptyList(),
    val dayOfWeekDistribution: List<DayOfWeekBucket> = emptyList(),
    val timeSegments: TimeSegmentSummary = TimeSegmentSummary(),
    val topApps: List<AppAnalyticsItem> = emptyList(),
    val statusStats: List<StatusStat> = emptyList()
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val _selectedTimeRange = MutableStateFlow(AnalyticsTimeRange.LAST_7_DAYS)
    val selectedTimeRange: StateFlow<AnalyticsTimeRange> = _selectedTimeRange.asStateFlow()

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllNotifications(),
        _selectedTimeRange
    ) { allNotifications, timeRange ->
        computeAnalytics(allNotifications, timeRange)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onTimeRangeSelected(range: AnalyticsTimeRange) {
        _selectedTimeRange.value = range
    }

    private fun computeAnalytics(
        allList: List<NotificationRecordEntity>,
        timeRange: AnalyticsTimeRange
    ): AnalyticsUiState {
        val now = System.currentTimeMillis()
        val startOfToday = DateTimeUtils.getStartOfDay(now)

        val (filtered, daysCount) = when (timeRange) {
            AnalyticsTimeRange.TODAY -> {
                Pair(allList.filter { it.captureTime >= startOfToday }, 1)
            }
            AnalyticsTimeRange.LAST_7_DAYS -> {
                val sevenDaysAgo = now - 7L * 24 * 60 * 60 * 1000
                Pair(allList.filter { it.captureTime >= sevenDaysAgo }, 7)
            }
            AnalyticsTimeRange.LAST_30_DAYS -> {
                val thirtyDaysAgo = now - 30L * 24 * 60 * 60 * 1000
                Pair(allList.filter { it.captureTime >= thirtyDaysAgo }, 30)
            }
            AnalyticsTimeRange.ALL_TIME -> {
                val oldest = allList.minOfOrNull { it.captureTime } ?: now
                val daysDiff = ((now - oldest) / (24L * 60 * 60 * 1000)).toInt().coerceAtLeast(1)
                Pair(allList, daysDiff)
            }
        }

        val total = filtered.size
        val totalFloat = if (total > 0) total.toFloat() else 1f
        val dailyAvg = if (daysCount > 0) total.toFloat() / daysCount else 0f

        val cal = Calendar.getInstance()

        // 1. Hourly distribution (0..23)
        val hourCounts = IntArray(24)
        var quietHours = 0
        var morning = 0
        var afternoon = 0
        var evening = 0
        var night = 0

        filtered.forEach { item ->
            cal.timeInMillis = item.postTime
            val h = cal.get(Calendar.HOUR_OF_DAY)
            if (h in 0..23) {
                hourCounts[h]++
            }
            // Quiet hours: 11 PM to 7 AM (23..23 or 0..6)
            if (h == 23 || h < 7) {
                quietHours++
            }
            // Time segments
            when (h) {
                in 6..11 -> morning++
                in 12..17 -> afternoon++
                in 18..22 -> evening++
                else -> night++
            }
        }

        var peakHour: Int? = null
        var maxHourCount = 0
        for (h in 0..23) {
            if (hourCounts[h] > maxHourCount) {
                maxHourCount = hourCounts[h]
                peakHour = h
            }
        }

        val hourlyBuckets = (0..23).map { h ->
            val label = when (h) {
                0 -> "12A"
                in 1..11 -> "${h}A"
                12 -> "12P"
                else -> "${h - 12}P"
            }
            HourlyBucket(hour = h, count = hourCounts[h], displayLabel = label)
        }

        // 2. Day of Week distribution (Sun..Sat)
        val dowCounts = IntArray(8)
        filtered.forEach { item ->
            cal.timeInMillis = item.postTime
            val dow = cal.get(Calendar.DAY_OF_WEEK)
            if (dow in 1..7) {
                dowCounts[dow]++
            }
        }

        val dayNames = listOf(
            Pair(Calendar.SUNDAY, "Sun"),
            Pair(Calendar.MONDAY, "Mon"),
            Pair(Calendar.TUESDAY, "Tue"),
            Pair(Calendar.WEDNESDAY, "Wed"),
            Pair(Calendar.THURSDAY, "Thu"),
            Pair(Calendar.FRIDAY, "Fri"),
            Pair(Calendar.SATURDAY, "Sat")
        )
        val dayOfWeekBuckets = dayNames.map { (dow, name) ->
            val count = dowCounts[dow]
            DayOfWeekBucket(
                dayOfWeek = dow,
                shortName = name,
                count = count,
                percentage = (count / totalFloat) * 100f
            )
        }

        // 3. App ranking
        val appGroups = filtered.groupBy { it.packageName }
        val appAnalytics = appGroups.map { (pkg, list) ->
            val count = list.size
            AppAnalyticsItem(
                packageName = pkg,
                appName = list.firstOrNull()?.appName ?: pkg,
                notificationCount = count,
                percentage = (count / totalFloat) * 100f,
                dailyAverage = if (daysCount > 0) count.toFloat() / daysCount else count.toFloat()
            )
        }.sortedByDescending { it.notificationCount }

        // 4. Status Breakdown
        val statusCounts = filtered.groupingBy { it.interactionStatus }.eachCount()
        val statusStats = InteractionStatus.values().mapNotNull { status ->
            val count = statusCounts[status] ?: 0
            if (count > 0) {
                StatusStat(
                    status = status,
                    count = count,
                    percentage = (count / totalFloat) * 100f
                )
            } else null
        }.sortedByDescending { it.count }

        return AnalyticsUiState(
            timeRange = timeRange,
            totalCount = total,
            distinctAppsCount = appGroups.size,
            dailyAverage = dailyAvg,
            peakHour = if (maxHourCount > 0) peakHour else null,
            peakHourCount = maxHourCount,
            quietHoursCount = quietHours,
            quietHoursPercentage = (quietHours / totalFloat) * 100f,
            topNoisiestApp = appAnalytics.firstOrNull(),
            hourlyDistribution = hourlyBuckets,
            dayOfWeekDistribution = dayOfWeekBuckets,
            timeSegments = TimeSegmentSummary(
                morningCount = morning,
                afternoonCount = afternoon,
                eveningCount = evening,
                nightCount = night
            ),
            topApps = appAnalytics,
            statusStats = statusStats
        )
    }
}
