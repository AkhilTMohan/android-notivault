package com.bc.notivault.data.repository

import com.bc.notivault.data.db.NotificationDao
import com.bc.notivault.data.model.AppStatInfo
import com.bc.notivault.data.model.InteractionStatus
import com.bc.notivault.data.model.NotificationRecordEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface NotificationRepository {
    fun getAllNotifications(): Flow<List<NotificationRecordEntity>>
    fun searchNotifications(query: String): Flow<List<NotificationRecordEntity>>
    fun getNotificationById(id: Long): Flow<NotificationRecordEntity?>
    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationRecordEntity>>
    fun getNotificationsByPackageAndDateRange(packageName: String, startTime: Long, endTime: Long): Flow<List<NotificationRecordEntity>>
    fun getNotificationsByDateRange(startTime: Long, endTime: Long): Flow<List<NotificationRecordEntity>>
    fun getStarredNotifications(): Flow<List<NotificationRecordEntity>>
    fun getAppStats(): Flow<List<AppStatInfo>>
    fun getTotalCount(): Flow<Int>
    fun getTodayCount(startOfDay: Long): Flow<Int>
    fun getActiveOngoingCount(): Flow<Int>
    suspend fun toggleStar(id: Long, isStarred: Boolean)
    suspend fun deleteNotification(id: Long)
    suspend fun deleteAllNotifications()
    suspend fun deleteOlderThanDays(days: Int): Int
    suspend fun exportNotificationsJson(): String
    suspend fun exportNotificationsPlainText(): String
    suspend fun recordOrUpdateNotification(entity: NotificationRecordEntity): Long
    suspend fun recordRemoval(
        notificationKey: String,
        removalTime: Long,
        reason: Int,
        reasonText: String,
        status: InteractionStatus
    )
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao
) : NotificationRepository {

    override fun getAllNotifications(): Flow<List<NotificationRecordEntity>> =
        notificationDao.getAll()

    override fun searchNotifications(query: String): Flow<List<NotificationRecordEntity>> =
        notificationDao.search(query)

    override fun getNotificationById(id: Long): Flow<NotificationRecordEntity?> =
        notificationDao.getById(id)

    override fun getNotificationsByPackage(packageName: String): Flow<List<NotificationRecordEntity>> =
        notificationDao.getByPackage(packageName)

    override fun getNotificationsByPackageAndDateRange(
        packageName: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<NotificationRecordEntity>> =
        notificationDao.getByPackageAndDateRange(packageName, startTime, endTime)

    override fun getNotificationsByDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<NotificationRecordEntity>> =
        notificationDao.getByDateRange(startTime, endTime)

    override fun getStarredNotifications(): Flow<List<NotificationRecordEntity>> =
        notificationDao.getStarred()

    override fun getAppStats(): Flow<List<AppStatInfo>> =
        notificationDao.getAppStats()

    override fun getTotalCount(): Flow<Int> =
        notificationDao.getTotalCount()

    override fun getTodayCount(startOfDay: Long): Flow<Int> =
        notificationDao.getTodayCount(startOfDay)

    override fun getActiveOngoingCount(): Flow<Int> =
        notificationDao.getActiveOngoingCount()

    override suspend fun toggleStar(id: Long, isStarred: Boolean) {
        notificationDao.setStarred(id, isStarred)
    }

    override suspend fun deleteNotification(id: Long) {
        notificationDao.deleteById(id)
    }

    override suspend fun deleteAllNotifications() {
        notificationDao.deleteAll()
    }

    override suspend fun deleteOlderThanDays(days: Int): Int {
        if (days <= 0) return 0
        val cutoff = System.currentTimeMillis() - (days.toLong() * 24 * 60 * 60 * 1000)
        return notificationDao.deleteOlderThan(cutoff)
    }

    override suspend fun recordOrUpdateNotification(entity: NotificationRecordEntity): Long {
        val existing = notificationDao.findActiveByKey(entity.notificationKey)
        return if (existing != null) {
            // Check if contents are updated or unchanged
            val updated = existing.copy(
                title = entity.title ?: existing.title,
                text = entity.text ?: existing.text,
                bigText = entity.bigText ?: existing.bigText,
                subText = entity.subText ?: existing.subText,
                summaryText = entity.summaryText ?: existing.summaryText,
                conversationTitle = entity.conversationTitle ?: existing.conversationTitle,
                category = entity.category ?: existing.category,
                channelId = entity.channelId ?: existing.channelId,
                channelName = entity.channelName ?: existing.channelName,
                isOngoing = entity.isOngoing,
                lastUpdateTime = entity.lastUpdateTime,
                interactionStatus = InteractionStatus.UPDATED,
                hasImageAttachment = entity.hasImageAttachment || existing.hasImageAttachment,
                imageFilePath = entity.imageFilePath ?: existing.imageFilePath
            )
            notificationDao.update(updated)
            existing.id
        } else {
            notificationDao.insert(entity)
        }
    }

    override suspend fun recordRemoval(
        notificationKey: String,
        removalTime: Long,
        reason: Int,
        reasonText: String,
        status: InteractionStatus
    ) {
        val existing = notificationDao.findActiveByKey(notificationKey)
            ?: notificationDao.findLatestByKey(notificationKey)

        if (existing != null) {
            val updated = existing.copy(
                removalTime = removalTime,
                removalReason = reason,
                removalReasonText = reasonText,
                interactionStatus = status,
                lastUpdateTime = removalTime
            )
            notificationDao.update(updated)
        }
    }

    override suspend fun exportNotificationsJson(): String {
        val all = notificationDao.getAllSync()
        val jsonArray = JSONArray()
        for (item in all) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("packageName", item.packageName)
            obj.put("appName", item.appName)
            obj.put("title", item.title ?: "")
            obj.put("text", item.text ?: "")
            obj.put("bigText", item.bigText ?: "")
            obj.put("subText", item.subText ?: "")
            obj.put("channelId", item.channelId ?: "")
            obj.put("category", item.category ?: "")
            obj.put("postTime", item.postTime)
            obj.put("captureTime", item.captureTime)
            obj.put("removalTime", item.removalTime ?: 0L)
            obj.put("interactionStatus", item.interactionStatus.name)
            obj.put("isStarred", item.isStarred)
            obj.put("isOngoing", item.isOngoing)
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    override suspend fun exportNotificationsPlainText(): String {
        val all = notificationDao.getAllSync()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("=== NotiVault Notification History Export ===\n")
        sb.append("Total Records: ${all.size}\n")
        sb.append("Export Time: ${dateFormat.format(Date())}\n\n")

        for (item in all) {
            sb.append("--------------------------------------------------\n")
            sb.append("App: ${item.appName} (${item.packageName})\n")
            sb.append("Date: ${dateFormat.format(Date(item.postTime))}\n")
            sb.append("Status: ${item.interactionStatus.displayName}\n")
            if (!item.title.isNullOrBlank()) sb.append("Title: ${item.title}\n")
            if (!item.text.isNullOrBlank()) sb.append("Text: ${item.text}\n")
            if (!item.bigText.isNullOrBlank() && item.bigText != item.text) {
                sb.append("Expanded: ${item.bigText}\n")
            }
            if (!item.channelId.isNullOrBlank()) sb.append("Channel: ${item.channelId}\n")
            if (item.removalTime != null) {
                sb.append("Removed: ${dateFormat.format(Date(item.removalTime))} (Reason: ${item.removalReasonText ?: "Unknown"})\n")
            }
            sb.append("\n")
        }
        return sb.toString()
    }
}
