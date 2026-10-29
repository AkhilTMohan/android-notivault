package com.bc.notivault.ads

object AdConfig {
    // Production Ad Unit IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-6629937725996361/6561498245"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6629937725996361/3328830245"

    // Cooldown duration between interstitial ads to comply with AdMob policy (60 seconds)
    const val INTERSTITIAL_COOLDOWN_MS = 60_000L

    // Action frequency: trigger after every N natural transitions
    const val INTERSTITIAL_ACTION_THRESHOLD = 3
}
