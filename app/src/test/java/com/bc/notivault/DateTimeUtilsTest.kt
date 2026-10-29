package com.bc.notivault

import com.bc.notivault.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    @Test
    fun testFormatRelativeTime() {
        val now = System.currentTimeMillis()
        val justNow = DateTimeUtils.formatRelativeTime(now - 10_000)
        assertEquals("Just now", justNow)

        val minutesAgo = DateTimeUtils.formatRelativeTime(now - (5 * 60 * 1000))
        assertEquals("5m ago", minutesAgo)

        val hoursAgo = DateTimeUtils.formatRelativeTime(now - (3 * 3600 * 1000))
        assertEquals("3h ago", hoursAgo)
    }

    @Test
    fun testGetDateGroupHeader() {
        val now = System.currentTimeMillis()
        val todayHeader = DateTimeUtils.getDateGroupHeader(now)
        assertEquals("Today", todayHeader)

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayHeader = DateTimeUtils.getDateGroupHeader(cal.timeInMillis)
        assertEquals("Yesterday", yesterdayHeader)
    }

    @Test
    fun testGetStartOfDay() {
        val now = System.currentTimeMillis()
        val startOfDay = DateTimeUtils.getStartOfDay(now)

        assertTrue("Start of day must be before or equal to now", startOfDay <= now)
        assertTrue("Difference between now and start of day must be less than 24 hours", now - startOfDay < 24 * 3600 * 1000)

        val cal = Calendar.getInstance().apply { timeInMillis = startOfDay }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
        assertEquals(0, cal.get(Calendar.MILLISECOND))
    }
}
