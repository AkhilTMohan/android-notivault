package com.bc.notivault

import com.bc.notivault.ads.AdConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdMobConfigTest {

    @Test
    fun testAdUnitIdsConfigured() {
        assertEquals("ca-app-pub-6629937725996361/6561498245", AdConfig.PROD_BANNER_AD_UNIT_ID)
        assertEquals("ca-app-pub-6629937725996361/3328830245", AdConfig.PROD_INTERSTITIAL_AD_UNIT_ID)
        assertTrue(AdConfig.BANNER_AD_UNIT_ID.isNotEmpty())
        assertTrue(AdConfig.INTERSTITIAL_AD_UNIT_ID.isNotEmpty())
    }

    @Test
    fun testPolicyThresholds() {
        // Cooldown between interstitial impressions should be at least 60 seconds
        assertTrue(AdConfig.INTERSTITIAL_COOLDOWN_MS >= 60_000L)
        // Action threshold enabled on major clicks
        assertTrue(AdConfig.INTERSTITIAL_ACTION_THRESHOLD >= 1)
    }
}
