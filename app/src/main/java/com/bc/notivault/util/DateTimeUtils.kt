package com.bc.notivault.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateTimeUtils {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("MMM d, yyyy • h:mm:ss a", Locale.getDefault())
    private val headerDateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatRelativeTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        if (diff < 0) return "Just now"

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days == 1L -> "Yesterday ${timeFormat.format(Date(timestamp))}"
            days < 7 -> "${days}d ago • ${timeFormat.format(Date(timestamp))}"
            else -> fullDateFormat.format(Date(timestamp))
        }
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }

    fun formatFullDate(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatFullDateTime(timestamp: Long): String {
        return fullDateFormat.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    fun getMonthYearTitle(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time)
    }

    fun getDateGroupHeader(timestamp: Long): String {
        val targetCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val nowCal = Calendar.getInstance()

        val isSameYear = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
        val targetDay = targetCal.get(Calendar.DAY_OF_YEAR)
        val nowDay = nowCal.get(Calendar.DAY_OF_YEAR)

        return if (isSameYear) {
            when (targetDay) {
                nowDay -> "Today"
                nowDay - 1 -> "Yesterday"
                else -> headerDateFormat.format(Date(timestamp))
            }
        } else {
            headerDateFormat.format(Date(timestamp))
        }
    }

    fun getStartOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
