package com.bc.notivault.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "notivault_preferences")

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyOnboardingCompleted = booleanPreferencesKey("onboarding_completed")
    private val keyExcludedPackages = stringSetPreferencesKey("excluded_packages")
    private val keyRetentionDays = intPreferencesKey("retention_days")
    private val keyThemeMode = stringPreferencesKey("theme_mode")

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[keyOnboardingCompleted] ?: false
    }

    val excludedPackages: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[keyExcludedPackages] ?: emptySet()
    }

    val retentionDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[keyRetentionDays] ?: 30
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[keyThemeMode] ?: "SYSTEM"
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[keyOnboardingCompleted] = completed
        }
    }

    suspend fun setExcludedPackages(packages: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[keyExcludedPackages] = packages
        }
    }

    suspend fun toggleExcludedPackage(packageName: String, exclude: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[keyExcludedPackages] ?: emptySet()
            preferences[keyExcludedPackages] = if (exclude) {
                current + packageName
            } else {
                current - packageName
            }
        }
    }

    suspend fun setRetentionDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[keyRetentionDays] = days
        }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[keyThemeMode] = mode
        }
    }
}
