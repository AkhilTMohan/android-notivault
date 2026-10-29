package com.bc.notivault.ui.screens.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppItemState(
    val packageName: String,
    val appName: String,
    val notificationCount: Int,
    val isExcluded: Boolean
)

data class AppsUiState(
    val apps: List<AppItemState> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false
)

@HiltViewModel
class AppsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: NotificationRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _installedApps = MutableStateFlow<List<Pair<String, String>>>(emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) {
                try {
                    val pm = context.packageManager
                    val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getInstalledApplications(0)
                    }

                    apps.filter {
                        // Filter out NotiVault itself and only include launchable or user apps
                        it.packageName != context.packageName &&
                                (it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || pm.getLaunchIntentForPackage(it.packageName) != null)
                    }.map {
                        Pair(it.packageName, pm.getApplicationLabel(it).toString())
                    }.sortedBy { it.second.lowercase() }
                } catch (_: Exception) {
                    emptyList()
                }
            }
            _installedApps.value = list
        }
    }

    val uiState: StateFlow<AppsUiState> = combine(
        _installedApps,
        repository.getAppStats(),
        preferenceManager.excludedPackages,
        _searchQuery
    ) { installed, stats, excluded, query ->
        val statsMap = stats.associateBy({ it.packageName }, { it.notificationCount })

        // Combine installed apps with any other app packages recorded in stats
        val allPackages = (installed.map { it.first } + stats.map { it.packageName }).distinct()

        val items = allPackages.map { pkg ->
            val appName = installed.find { it.first == pkg }?.second
                ?: stats.find { it.packageName == pkg }?.appName
                ?: pkg
            val count = statsMap[pkg] ?: 0
            val isExcluded = excluded.contains(pkg)

            AppItemState(
                packageName = pkg,
                appName = appName,
                notificationCount = count,
                isExcluded = isExcluded
            )
        }.sortedWith(
            compareByDescending<AppItemState> { it.isExcluded }
                .thenByDescending { it.notificationCount }
                .thenBy { it.appName.lowercase() }
        ).filter {
            if (query.isBlank()) true
            else it.appName.lowercase().contains(query.trim().lowercase()) ||
                    it.packageName.lowercase().contains(query.trim().lowercase())
        }

        AppsUiState(
            apps = items,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppsUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun toggleAppExclusion(packageName: String, exclude: Boolean) {
        viewModelScope.launch {
            preferenceManager.toggleExcludedPackage(packageName, exclude)
        }
    }
}
