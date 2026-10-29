package com.bc.notivault

import com.bc.notivault.ui.navigation.Destinations
import com.bc.notivault.util.DateTimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalendarAndAppNavigationTest {

    @Test
    fun testAppNotificationsRouteFormatting() {
        val routeWithoutDate = Destinations.appNotificationsRoute("com.whatsapp")
        assertEquals("app_notifications/com.whatsapp", routeWithoutDate)

        val timestamp = 1793264400000L
        val routeWithDate = Destinations.appNotificationsRoute("com.whatsapp", timestamp)
        assertEquals("app_notifications/com.whatsapp?date=$timestamp", routeWithDate)
    }

    @Test
    fun testMonthYearTitleAndDateRange() {
        val title = DateTimeUtils.getMonthYearTitle(2026, Calendar.OCTOBER)
        assertTrue(title.contains("October"))
        assertTrue(title.contains("2026"))

        val now = System.currentTimeMillis()
        val start = DateTimeUtils.getStartOfDay(now)
        val end = DateTimeUtils.getEndOfDay(now)

        assertTrue(start < end)
        assertTrue(end - start == 86399999L) // Exactly 23h 59m 59s 999ms
    }
}
