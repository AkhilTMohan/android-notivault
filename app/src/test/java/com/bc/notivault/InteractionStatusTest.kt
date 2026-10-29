package com.bc.notivault

import com.bc.notivault.data.db.Converters
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InteractionStatusTest {

    @Test
    fun testInteractionStatusConfirmedFlags() {
        assertTrue("POSTED should be confirmed", InteractionStatus.POSTED.isConfirmed)
        assertTrue("UPDATED should be confirmed", InteractionStatus.UPDATED.isConfirmed)
        assertTrue("REMOVED should be confirmed", InteractionStatus.REMOVED.isConfirmed)
        assertTrue("DISMISSED should be confirmed", InteractionStatus.DISMISSED.isConfirmed)
        assertTrue("OPENED should be confirmed", InteractionStatus.OPENED.isConfirmed)
        assertFalse("UNKNOWN should be unconfirmed", InteractionStatus.UNKNOWN.isConfirmed)
    }

    @Test
    fun testTypeConverters() {
        val converters = Converters()

        assertEquals("POSTED", converters.fromInteractionStatus(InteractionStatus.POSTED))
        assertEquals(InteractionStatus.POSTED, converters.toInteractionStatus("POSTED"))

        assertEquals("DISMISSED", converters.fromInteractionStatus(InteractionStatus.DISMISSED))
        assertEquals(InteractionStatus.DISMISSED, converters.toInteractionStatus("DISMISSED"))

        assertEquals("OPENED", converters.fromInteractionStatus(InteractionStatus.OPENED))
        assertEquals(InteractionStatus.OPENED, converters.toInteractionStatus("OPENED"))

        // Invalid fallback to UNKNOWN
        assertEquals(InteractionStatus.UNKNOWN, converters.toInteractionStatus("INVALID_STATUS"))
        assertEquals(InteractionStatus.UNKNOWN, converters.toInteractionStatus(null))
    }

    @Test
    fun testNotificationRecordEntityCreation() {
        val now = System.currentTimeMillis()
        val record = NotificationRecordEntity(
            id = 1L,
            notificationKey = "com.test.app_1_12345",
            packageName = "com.test.app",
            appName = "Test App",
            title = "Test Title",
            text = "Test Text",
            postTime = now,
            captureTime = now,
            lastUpdateTime = now,
            interactionStatus = InteractionStatus.POSTED
        )

        assertEquals("com.test.app", record.packageName)
        assertEquals("Test App", record.appName)
        assertEquals("Test Title", record.title)
        assertEquals("Test Text", record.text)
        assertEquals(InteractionStatus.POSTED, record.interactionStatus)
        assertFalse(record.isStarred)
        assertFalse(record.isOngoing)
    }
}
