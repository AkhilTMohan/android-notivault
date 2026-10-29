package com.bc.notivault.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import javax.inject.Inject

data class DayAppInfo(
    val packageName: String,
    val appName: String,
    val count: Int
)

data class DayData(
    val dayOfMonth: Int,
    val dateTimestamp: Long,
    val totalCount: Int,
    val topPackageNames: List<String>,
    val apps: List<DayAppInfo>
)

data class CalendarUiState(
    val year: Int,
    val month: Int, // 0-indexed
    val daysInMonth: Int,
    val firstDayOfWeek: Int, // 1 = Sunday, 2 = Monday, etc.
    val daysData: Map<Int, DayData> = emptyMap(),
    val selectedDay: DayData? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val repository: NotificationRepository
) : ViewModel() {

    private val now = Calendar.getInstance()
    private val _year = MutableStateFlow(now.get(Calendar.YEAR))
    private val _month = MutableStateFlow(now.get(Calendar.MONTH))
    private val _selectedDay = MutableStateFlow<DayData?>(null)

    val selectedDay: StateFlow<DayData?> = _selectedDay.asStateFlow()

    private val monthRange = MutableStateFlow(calculateMonthRange(_year.value, _month.value))

    val uiState: StateFlow<CalendarUiState> = monthRange.flatMapLatest { (start, end, y, m, daysCount, firstDow) ->
        repository.getNotificationsByDateRange(start, end).map { notifications ->
            val cal = Calendar.getInstance()
            val groupedByDay = notifications.groupBy {
                cal.timeInMillis = it.captureTime
                cal.get(Calendar.DAY_OF_MONTH)
            }

            val daysMap = mutableMapOf<Int, DayData>()
            for (day in 1..daysCount) {
                cal.set(Calendar.YEAR, y)
                cal.set(Calendar.MONTH, m)
                cal.set(Calendar.DAY_OF_MONTH, day)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val dayTimestamp = cal.timeInMillis

                val dayList = groupedByDay[day] ?: emptyList()
                val totalCount = dayList.size

                val appCounts = dayList.groupBy { it.packageName }.map { (pkg, list) ->
                    DayAppInfo(
                        packageName = pkg,
                        appName = list.firstOrNull()?.appName ?: pkg,
                        count = list.size
                    )
                }.sortedByDescending { it.count }

                val topPkgs = appCounts.take(3).map { it.packageName }

                daysMap[day] = DayData(
                    dayOfMonth = day,
                    dateTimestamp = dayTimestamp,
                    totalCount = totalCount,
                    topPackageNames = topPkgs,
                    apps = appCounts
                )
            }

            CalendarUiState(
                year = y,
                month = m,
                daysInMonth = daysCount,
                firstDayOfWeek = firstDow,
                daysData = daysMap,
                selectedDay = _selectedDay.value
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = run {
            val r = calculateMonthRange(_year.value, _month.value)
            CalendarUiState(
                year = r.year,
                month = r.month,
                daysInMonth = r.daysCount,
                firstDayOfWeek = r.firstDayOfWeek
            )
        }
    )

    fun onPreviousMonth() {
        if (_month.value == 0) {
            _month.value = 11
            _year.value = _year.value - 1
        } else {
            _month.value = _month.value - 1
        }
        monthRange.value = calculateMonthRange(_year.value, _month.value)
        _selectedDay.value = null
    }

    fun onNextMonth() {
        if (_month.value == 11) {
            _month.value = 0
            _year.value = _year.value + 1
        } else {
            _month.value = _month.value + 1
        }
        monthRange.value = calculateMonthRange(_year.value, _month.value)
        _selectedDay.value = null
    }

    fun onSelectDay(dayData: DayData?) {
        _selectedDay.value = dayData
    }

    private data class MonthInfo(
        val start: Long,
        val end: Long,
        val year: Int,
        val month: Int,
        val daysCount: Int,
        val firstDayOfWeek: Int
    )

    private fun calculateMonthRange(year: Int, month: Int): MonthInfo {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)
        val daysCount = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        cal.set(Calendar.DAY_OF_MONTH, daysCount)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        return MonthInfo(
            start = start,
            end = end,
            year = year,
            month = month,
            daysCount = daysCount,
            firstDayOfWeek = firstDow
        )
    }
}
