package com.bc.notivault.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * Persistent Google AdMob Banner Ad Composable.
 * Uses adaptive banner sizing to adjust cleanly to device orientation and width.
 * Manages pause, resume, and destroy lifecycle events according to AdMob best practices.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConfig.BANNER_AD_UNIT_ID
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp

    val adSize = remember(screenWidth) {
        if (screenWidth > 0) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidth)
        } else {
            AdSize.BANNER
        }
    }

    var adViewInstance by remember { mutableStateOf<AdView?>(null) }

    // Lifecycle management according to AdMob best practices
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        adViewInstance?.pause()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        adViewInstance?.resume()
    }

    // Teardown / cleanup when leaving composition
    DisposableEffect(Unit) {
        onDispose {
            adViewInstance?.destroy()
            adViewInstance = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
            thickness = 0.8.dp
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    Log.d("BannerAdView", "Loading banner ad with unit ID: $adUnitId")
                    AdView(ctx).apply {
                        setAdSize(adSize)
                        this.adUnitId = adUnitId
                        adListener = object : AdListener() {
                            override fun onAdLoaded() {
                                Log.d("BannerAdView", "Banner ad loaded successfully.")
                            }

                            override fun onAdFailedToLoad(error: LoadAdError) {
                                Log.w("BannerAdView", "Banner ad failed to load: ${error.message} (code: ${error.code})")
                            }
                        }
                        loadAd(AdRequest.Builder().build())
                        adViewInstance = this
                    }
                }
            )
        }
    }
}
