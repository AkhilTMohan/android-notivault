package com.bc.notivault.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_records",
    indices = [
        Index(value = ["notificationKey"]),
        Index(value = ["packageName"]),
        Index(value = ["postTime"]),
        Index(value = ["captureTime"]),
        Index(value = ["isStarred"]),
        Index(value = ["interactionStatus"])
    ]
)
data class NotificationRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationKey: String,
    val packageName: String,
    val appName: String,
    val title: String? = null,
    val text: String? = null,
    val bigText: String? = null,
    val subText: String? = null,
    val summaryText: String? = null,
    val conversationTitle: String? = null,
    val channelId: String? = null,
    val channelName: String? = null,
    val category: String? = null,
    val groupKey: String? = null,
    val isGroupSummary: Boolean = false,
    val isOngoing: Boolean = false,
    val postTime: Long,
    val captureTime: Long,
    val lastUpdateTime: Long,
    val removalTime: Long? = null,
    val removalReason: Int? = null,
    val removalReasonText: String? = null,
    val interactionStatus: InteractionStatus = InteractionStatus.POSTED,
    val isStarred: Boolean = false,
    val hasImageAttachment: Boolean = false,
    val imageFilePath: String? = null
)
