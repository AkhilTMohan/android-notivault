package com.bc.notivault.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bc.notivault.data.model.NotificationRecordEntity

@Database(
    entities = [NotificationRecordEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NotiVaultDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        const val DATABASE_NAME = "notivault.db"
    }
}
