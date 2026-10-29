package com.bc.notivault.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.bc.notivault.BuildConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private companion object {
        const val TAG = "AdManager"
    }

    private var interstitialAd: InterstitialAd? = null
    private val isInterstitialLoading = AtomicBoolean(false)
    private val actionCounter = AtomicInteger(0)
    private var lastInterstitialShownTimestamp: Long = 0L
    private val isInitialized = AtomicBoolean(false)

    /**
     * Initializes Google Mobile Ads SDK. Safe to call multiple times.
     */
    fun initialize() {
        if (isInitialized.compareAndSet(false, true)) {
            if (BuildConfig.DEBUG) {
                val testDeviceIds = listOf(
                    "BF9E2CC7EE5ED379FAA04A7448AB38CE",
                    AdRequest.DEVICE_ID_EMULATOR
                )
                val config = RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
                MobileAds.setRequestConfiguration(config)
            }

            MobileAds.initialize(context) { status ->
                Log.d(TAG, "Google Mobile Ads SDK initialized: $status")
                preloadInterstitial()
            }
        }
    }

    /**
     * Preloads an interstitial ad in the background if one is not already loaded or loading.
     */
    fun preloadInterstitial() {
        if (interstitialAd != null || isInterstitialLoading.get()) {
            return
        }

        isInterstitialLoading.set(true)
        val adRequest = AdRequest.Builder().build()
        val adUnitId = AdConfig.INTERSTITIAL_AD_UNIT_ID
        Log.d(TAG, "Loading interstitial ad with ID: $adUnitId")

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading.set(false)
                    Log.d(TAG, "Interstitial ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading.set(false)
                    Log.w(TAG, "Interstitial ad failed to load: ${loadAdError.message} (code: ${loadAdError.code})")
                }
            }
        )
    }

    /**
     * Shows an interstitial ad at natural transition points with full compliance to AdMob policies:
     * - Cooldown period (minimum 60s) between consecutive interstitial impressions.
     * - Action frequency capping (governed by threshold).
     * - Immediate execution of [onComplete] if ad is not ready, fails, or after dismissal,
     *   ensuring no blocking or disruption to the user experience.
     */
    fun showInterstitialIfAppropriate(activity: Activity, onComplete: () -> Unit) {
        val count = actionCounter.incrementAndGet()
        val currentTime = SystemClock.elapsedRealtime()
        val timeSinceLastAd = currentTime - lastInterstitialShownTimestamp

        val isEligible = count >= AdConfig.INTERSTITIAL_ACTION_THRESHOLD &&
                (lastInterstitialShownTimestamp == 0L || timeSinceLastAd >= AdConfig.INTERSTITIAL_COOLDOWN_MS)

        val currentAd = interstitialAd
        if (isEligible && currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed.")
                    interstitialAd = null
                    preloadInterstitial()
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    preloadInterstitial()
                    onComplete()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad presented.")
                    lastInterstitialShownTimestamp = SystemClock.elapsedRealtime()
                    actionCounter.set(0)
                }
            }
            currentAd.show(activity)
        } else {
            Log.d(TAG, "Interstitial skipped (eligible=$isEligible, ready=${currentAd != null}, count=$count, timeSinceLast=${timeSinceLastAd}ms)")
            if (currentAd == null && !isInterstitialLoading.get()) {
                preloadInterstitial()
            }
            onComplete()
        }
    }
}
