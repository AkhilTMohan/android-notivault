package com.bc.notivault.ads

import com.bc.notivault.BuildConfig

object AdConfig {
    // Official Google AdMob sample test ad unit IDs (guaranteed fill during development/testing)
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Production Ad Unit IDs
    const val PROD_BANNER_AD_UNIT_ID = "ca-app-pub-6629937725996361/6561498245"
    const val PROD_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6629937725996361/3328830245"

    /**
     * Active Banner Ad Unit ID:
     * In DEBUG builds, returns Google's official test unit ID so test ads appear immediately without No-Fill errors.
     * In RELEASE builds, returns the live production ad unit ID.
     */
    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_AD_UNIT_ID else PROD_BANNER_AD_UNIT_ID

    /**
     * Active Interstitial Ad Unit ID:
     * In DEBUG builds, returns Google's official test unit ID so test ads appear immediately without No-Fill errors.
     * In RELEASE builds, returns the live production ad unit ID.
     */
    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_AD_UNIT_ID else PROD_INTERSTITIAL_AD_UNIT_ID

    // Cooldown duration between interstitial ads to comply with AdMob policy (60 seconds)
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    // Action frequency: trigger on every major action (governed by the cooldown delay)
    const val INTERSTITIAL_ACTION_THRESHOLD = 1
}
