package com.bc.notivault

import android.app.Application
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.di.ApplicationScope
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NotiVaultApplication : Application() {

    @Inject
    lateinit var repository: NotificationRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    @ApplicationScope
    lateinit var applicationScope: CoroutineScope

    override fun onCreate() {
        super.onCreate()
        // Execute automatic retention cleanup on app startup in background
        applicationScope.launch {
            val retentionDays = preferenceManager.retentionDays.first()
            if (retentionDays > 0) {
                repository.deleteOlderThanDays(retentionDays)
            }
        }
    }
}
