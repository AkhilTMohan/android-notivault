package com.bc.notivault.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bc.notivault.ads.AdManager
import com.bc.notivault.ui.screens.analytics.AnalyticsScreen
import com.bc.notivault.ui.screens.analytics.AnalyticsViewModel
import com.bc.notivault.ui.screens.appnotifications.AppNotificationsScreen
import com.bc.notivault.ui.screens.appnotifications.AppNotificationsViewModel
import com.bc.notivault.ui.screens.apps.AppsScreen
import com.bc.notivault.ui.screens.apps.AppsViewModel
import com.bc.notivault.ui.screens.calendar.CalendarScreen
import com.bc.notivault.ui.screens.calendar.CalendarViewModel
import com.bc.notivault.ui.screens.dashboard.DashboardScreen
import com.bc.notivault.ui.screens.dashboard.DashboardViewModel
import com.bc.notivault.ui.screens.detail.DetailScreen
import com.bc.notivault.ui.screens.detail.DetailViewModel
import com.bc.notivault.ui.screens.onboarding.OnboardingScreen
import com.bc.notivault.ui.screens.settings.SettingsScreen
import com.bc.notivault.ui.screens.settings.SettingsViewModel
import com.bc.notivault.util.findActivity

object Destinations {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val DETAIL = "detail/{notificationId}"
    const val APPS = "apps"
    const val APP_NOTIFICATIONS = "app_notifications/{packageName}?date={date}"
    const val CALENDAR = "calendar"
    const val ANALYTICS = "analytics"
    const val SETTINGS = "settings"

    fun detailRoute(id: Long) = "detail/$id"

    fun appNotificationsRoute(packageName: String, date: Long? = null): String {
        return if (date != null) "app_notifications/$packageName?date=$date"
        else "app_notifications/$packageName"
    }
}

@Composable
fun NotiVaultNavHost(
    navController: NavHostController,
    startDestination: String,
    onCompleteOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
    adManager: AdManager? = null
) {
    val context = LocalContext.current
    val navigateWithAd: (() -> Unit) -> Unit = { navigationAction ->
        val activity = context.findActivity()
        if (adManager != null && activity != null) {
            adManager.showInterstitialIfAppropriate(activity, navigationAction)
        } else {
            navigationAction()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Destinations.ONBOARDING) {
            OnboardingScreen(
                onContinue = {
                    onCompleteOnboarding()
                    navController.navigate(Destinations.DASHBOARD) {
                        popUpTo(Destinations.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }

        composable(Destinations.DASHBOARD) {
            val viewModel: DashboardViewModel = hiltViewModel()
            DashboardScreen(
                viewModel = viewModel,
                onNavigateToAppNotifications = { pkg ->
                    navigateWithAd {
                        navController.navigate(Destinations.appNotificationsRoute(pkg))
                    }
                },
                onNavigateToDetail = { id ->
                    navigateWithAd {
                        navController.navigate(Destinations.detailRoute(id))
                    }
                },
                onNavigateToApps = {
                    navigateWithAd {
                        navController.navigate(Destinations.APPS)
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(Destinations.CALENDAR) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToAnalytics = {
                    navigateWithAd {
                        navController.navigate(Destinations.ANALYTICS)
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(
            route = Destinations.DETAIL,
            arguments = listOf(navArgument("notificationId") { type = NavType.LongType })
        ) {
            val viewModel: DetailViewModel = hiltViewModel()
            DetailScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Destinations.APPS) {
            val viewModel: AppsViewModel = hiltViewModel()
            AppsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAppNotifications = { pkg ->
                    navigateWithAd {
                        navController.navigate(Destinations.appNotificationsRoute(pkg))
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(Destinations.CALENDAR)
                }
            )
        }

        composable(
            route = Destinations.APP_NOTIFICATIONS,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("date") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) {
            val viewModel: AppNotificationsViewModel = hiltViewModel()
            AppNotificationsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToDetail = { id ->
                    navigateWithAd {
                        navController.navigate(Destinations.detailRoute(id))
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(Destinations.CALENDAR)
                }
            )
        }

        composable(Destinations.CALENDAR) {
            val viewModel: CalendarViewModel = hiltViewModel()
            CalendarScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAppNotifications = { pkg, date ->
                    navigateWithAd {
                        navController.navigate(Destinations.appNotificationsRoute(pkg, date))
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Destinations.DASHBOARD) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Destinations.SETTINGS) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        composable(Destinations.ANALYTICS) {
            val viewModel: AnalyticsViewModel = hiltViewModel()
            AnalyticsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAppNotifications = { pkg ->
                    navigateWithAd {
                        navController.navigate(Destinations.appNotificationsRoute(pkg))
                    }
                }
            )
        }

        composable(Destinations.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToApps = { navController.navigate(Destinations.APPS) },
                onNavigateToHome = {
                    navController.navigate(Destinations.DASHBOARD) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToCalendar = {
                    navController.navigate(Destinations.CALENDAR) {
                        popUpTo(Destinations.DASHBOARD) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}
