package com.bc.notivault

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.ui.navigation.Destinations
import com.bc.notivault.ui.navigation.NotiVaultNavHost
import com.bc.notivault.ui.theme.NotivaultTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isOnboardingCompleted by preferenceManager.isOnboardingCompleted.collectAsState(initial = null)
            val themeMode by preferenceManager.themeMode.collectAsState(initial = "SYSTEM")
            val coroutineScope = rememberCoroutineScope()

            // Wait until preference is read
            if (isOnboardingCompleted != null) {
                NotivaultTheme(themeMode = themeMode) {
                    val navController = rememberNavController()
                    val startDestination = if (isOnboardingCompleted == true) {
                        Destinations.DASHBOARD
                    } else {
                        Destinations.ONBOARDING
                    }

                    NotiVaultNavHost(
                        navController = navController,
                        startDestination = startDestination,
                        onCompleteOnboarding = {
                            coroutineScope.launch {
                                preferenceManager.setOnboardingCompleted(true)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
