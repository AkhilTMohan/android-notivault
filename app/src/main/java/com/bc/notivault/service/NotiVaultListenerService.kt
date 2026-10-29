package com.bc.notivault.service

import android.app.Notification
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NotiVaultListenerService : NotificationListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ServiceEntryPoint {
        fun repository(): NotificationRepository
        fun preferenceManager(): PreferenceManager
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var repository: NotificationRepository
    private lateinit var preferenceManager: PreferenceManager

    companion object {
        private const val TAG = "NotiVaultListener"
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        // Android NotificationListenerService Reason Constants
        const val REASON_CLICK = 1
        const val REASON_CANCEL = 2
        const val REASON_CANCEL_ALL = 3
        const val REASON_ERROR = 4
        const val REASON_PACKAGE_CHANGED = 5
        const val REASON_USER_STOPPED = 6
        const val REASON_PACKAGE_BANNED = 7
        const val REASON_APP_CANCEL = 8
        const val REASON_APP_CANCEL_ALL = 9
        const val REASON_LISTENER_CANCEL = 10
        const val REASON_LISTENER_CANCEL_ALL = 11
        const val REASON_GROUP_SUMMARY_CANCELED = 12
        const val REASON_GROUP_OPTIMIZATION = 13
        const val REASON_PACKAGE_SUSPENDED = 14
        const val REASON_PROFILE_TURNED_OFF = 15
        const val REASON_UNAUTOBUNDLED = 16
        const val REASON_CHANNEL_BANNED = 17
        const val REASON_SNOOZED = 18
        const val REASON_TIMEOUT = 19
        const val REASON_CHANNEL_REMOVED = 20
        const val REASON_CLEAR_DATA = 21
        const val REASON_ASSISTANT_CANCEL = 22
    }

    override fun onCreate() {
        super.onCreate()
        try {
            val entryPoint = EntryPointAccessors.fromApplication(
                applicationContext,
                ServiceEntryPoint::class.java
            )
            repository = entryPoint.repository()
            preferenceManager = entryPoint.preferenceManager()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing dependencies in NotiVaultListenerService", e)
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        _isServiceConnected.value = true
        Log.d(TAG, "NotiVault notification listener connected.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        _isServiceConnected.value = false
        Log.d(TAG, "NotiVault notification listener disconnected.")
    }

    override fun onDestroy() {
        _isServiceConnected.value = false
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        // Never record notifications from NotiVault itself
        if (pkgName == packageName) return

        serviceScope.launch {
            try {
                // Check if user excluded this app
                val excluded = preferenceManager.excludedPackages.first()
                if (excluded.contains(pkgName)) {
                    Log.d(TAG, "Ignoring notification from excluded package: $pkgName")
                    return@launch
                }

                val notification = sbn.notification ?: return@launch
                val extras = notification.extras ?: Bundle()

                val appName = getAppName(pkgName)
                val notificationKey = sbn.key ?: "${pkgName}_${sbn.id}_${sbn.postTime}"

                // Safely extract text fields
                val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
                val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
                val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
                val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
                val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()?.trim()
                val conversationTitle = extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()?.trim()

                // Channel information
                val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notification.channelId
                } else null

                // Flags
                val isOngoing = (notification.flags and Notification.FLAG_ONGOING_EVENT) != 0
                val isGroupSummary = (notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0
                val groupKey = sbn.groupKey

                val now = System.currentTimeMillis()
                val postTime = if (sbn.postTime > 0) sbn.postTime else now

                // Safe image thumbnail extraction (if available)
                val imagePath = extractAndSaveThumbnail(notification, notificationKey)

                val entity = NotificationRecordEntity(
                    notificationKey = notificationKey,
                    packageName = pkgName,
                    appName = appName,
                    title = title,
                    text = text,
                    bigText = bigText,
                    subText = subText,
                    summaryText = summaryText,
                    conversationTitle = conversationTitle,
                    channelId = channelId,
                    category = notification.category,
                    groupKey = groupKey,
                    isGroupSummary = isGroupSummary,
                    isOngoing = isOngoing,
                    postTime = postTime,
                    captureTime = now,
                    lastUpdateTime = now,
                    interactionStatus = InteractionStatus.POSTED,
                    hasImageAttachment = imagePath != null,
                    imageFilePath = imagePath
                )

                repository.recordOrUpdateNotification(entity)
                Log.d(TAG, "Captured notification from $appName: $title")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording notification for ${sbn.packageName}", e)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?, reason: Int) {
        if (sbn == null) return
        val pkgName = sbn.packageName ?: return
        if (pkgName == packageName) return

        serviceScope.launch {
            try {
                val notificationKey = sbn.key ?: "${pkgName}_${sbn.id}_${sbn.postTime}"
                val now = System.currentTimeMillis()

                val reasonText = mapRemovalReasonText(reason)
                val status = mapRemovalStatus(reason)

                repository.recordRemoval(
                    notificationKey = notificationKey,
                    removalTime = now,
                    reason = reason,
                    reasonText = reasonText,
                    status = status
                )
                Log.d(TAG, "Notification removed: $notificationKey reason: $reasonText ($status)")
            } catch (e: Exception) {
                Log.e(TAG, "Error recording removal for ${sbn.packageName}", e)
            }
        }
    }

    private fun getAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun extractAndSaveThumbnail(notification: Notification, key: String): String? {
        return try {
            val extras = notification.extras ?: return null
            var bitmap: Bitmap? = null

            // Check for BigPicture style image
            if (extras.containsKey(Notification.EXTRA_PICTURE)) {
                val pic = extras.get(Notification.EXTRA_PICTURE)
                if (pic is Bitmap) {
                    bitmap = pic
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && pic is Icon) {
                    val drawable = pic.loadDrawable(this)
                    if (drawable is BitmapDrawable) {
                        bitmap = drawable.bitmap
                    }
                }
            }

            if (bitmap == null && extras.containsKey(Notification.EXTRA_LARGE_ICON_BIG)) {
                val icon = extras.get(Notification.EXTRA_LARGE_ICON_BIG)
                if (icon is Bitmap) {
                    bitmap = icon
                }
            }

            if (bitmap == null) return null

            // Scale down to max 300x300 thumbnail to save disk space and memory
            val maxDim = 300
            val width = bitmap.width
            val height = bitmap.height
            val scale = (maxDim.toFloat() / maxOf(width, height)).coerceAtMost(1.0f)
            val scaledBitmap = if (scale < 1.0f) {
                Bitmap.createScaledBitmap(bitmap, (width * scale).toInt(), (height * scale).toInt(), true)
            } else {
                bitmap
            }

            val sanitizedKey = key.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
            val imageDir = File(filesDir, "notification_images")
            if (!imageDir.exists()) imageDir.mkdirs()

            val targetFile = File(imageDir, "img_${sanitizedKey}.jpg")
            FileOutputStream(targetFile).use { out ->
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            targetFile.absolutePath
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract notification thumbnail: ${e.message}")
            null
        }
    }

    private fun mapRemovalReasonText(reason: Int): String {
        return when (reason) {
            REASON_CLICK -> "Tapped by user"
            REASON_CANCEL -> "Dismissed by user swipe"
            REASON_CANCEL_ALL -> "Dismissed via Clear All"
            REASON_ERROR -> "Notification error"
            REASON_PACKAGE_CHANGED -> "App package changed/updated"
            REASON_USER_STOPPED -> "User stopped app"
            REASON_PACKAGE_BANNED -> "App package blocked"
            REASON_APP_CANCEL -> "Cancelled by originating app"
            REASON_APP_CANCEL_ALL -> "All cancelled by originating app"
            REASON_LISTENER_CANCEL -> "Cancelled by notification listener"
            REASON_LISTENER_CANCEL_ALL -> "All cancelled by listener"
            REASON_GROUP_SUMMARY_CANCELED -> "Group summary cancelled"
            REASON_GROUP_OPTIMIZATION -> "System group optimization"
            REASON_PACKAGE_SUSPENDED -> "App package suspended"
            REASON_PROFILE_TURNED_OFF -> "User profile turned off"
            REASON_UNAUTOBUNDLED -> "Unautobundled"
            REASON_CHANNEL_BANNED -> "Notification channel blocked"
            REASON_SNOOZED -> "Snoozed by user"
            REASON_TIMEOUT -> "Notification expired / timed out"
            REASON_CHANNEL_REMOVED -> "Notification channel deleted"
            REASON_CLEAR_DATA -> "App storage cleared"
            REASON_ASSISTANT_CANCEL -> "Cancelled by assistant"
            else -> "System event (code $reason)"
        }
    }

    private fun mapRemovalStatus(reason: Int): InteractionStatus {
        return when (reason) {
            REASON_CLICK -> InteractionStatus.OPENED
            REASON_CANCEL, REASON_CANCEL_ALL -> InteractionStatus.DISMISSED
            REASON_APP_CANCEL, REASON_APP_CANCEL_ALL,
            REASON_TIMEOUT, REASON_CHANNEL_REMOVED,
            REASON_CLEAR_DATA, REASON_ERROR,
            REASON_PACKAGE_CHANGED, REASON_PACKAGE_BANNED -> InteractionStatus.REMOVED
            else -> InteractionStatus.REMOVED
        }
    }
}
