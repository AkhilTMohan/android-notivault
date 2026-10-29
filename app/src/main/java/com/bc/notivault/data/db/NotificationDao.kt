package com.bc.notivault.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.bc.notivault.data.model.AppStatInfo
import com.bc.notivault.data.model.NotificationRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationRecordEntity): Long

    @Update
    suspend fun update(entity: NotificationRecordEntity)

    @Query("SELECT * FROM notification_records WHERE notificationKey = :key AND removalTime IS NULL ORDER BY captureTime DESC LIMIT 1")
    suspend fun findActiveByKey(key: String): NotificationRecordEntity?

    @Query("SELECT * FROM notification_records WHERE notificationKey = :key ORDER BY captureTime DESC LIMIT 1")
    suspend fun findLatestByKey(key: String): NotificationRecordEntity?

    @Query("SELECT * FROM notification_records WHERE id = :id LIMIT 1")
    fun getById(id: Long): Flow<NotificationRecordEntity?>

    @Query("SELECT * FROM notification_records WHERE id = :id LIMIT 1")
    suspend fun getByIdSync(id: Long): NotificationRecordEntity?

    @Query("SELECT * FROM notification_records ORDER BY captureTime DESC")
    fun getAll(): Flow<List<NotificationRecordEntity>>

    @Query("""
        SELECT * FROM notification_records 
        WHERE title LIKE '%' || :query || '%' 
           OR text LIKE '%' || :query || '%' 
           OR bigText LIKE '%' || :query || '%' 
           OR appName LIKE '%' || :query || '%'
           OR subText LIKE '%' || :query || '%'
        ORDER BY captureTime DESC
    """)
    fun search(query: String): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE packageName = :packageName ORDER BY captureTime DESC")
    fun getByPackage(packageName: String): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE packageName = :packageName AND captureTime >= :startTime AND captureTime <= :endTime ORDER BY captureTime DESC")
    fun getByPackageAndDateRange(packageName: String, startTime: Long, endTime: Long): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE captureTime >= :startTime AND captureTime <= :endTime ORDER BY captureTime DESC")
    fun getByDateRange(startTime: Long, endTime: Long): Flow<List<NotificationRecordEntity>>

    @Query("SELECT * FROM notification_records WHERE isStarred = 1 ORDER BY captureTime DESC")
    fun getStarred(): Flow<List<NotificationRecordEntity>>

    @Query("""
        SELECT packageName, appName, COUNT(*) AS notificationCount, MAX(postTime) AS latestPostTime 
        FROM notification_records 
        GROUP BY packageName 
        ORDER BY notificationCount DESC
    """)
    fun getAppStats(): Flow<List<AppStatInfo>>

    @Query("SELECT COUNT(*) FROM notification_records")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_records WHERE captureTime >= :startOfDay")
    fun getTodayCount(startOfDay: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_records WHERE isOngoing = 1 AND removalTime IS NULL")
    fun getActiveOngoingCount(): Flow<Int>

    @Query("DELETE FROM notification_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_records")
    suspend fun deleteAll()

    @Query("DELETE FROM notification_records WHERE captureTime < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long): Int

    @Query("UPDATE notification_records SET isStarred = :isStarred WHERE id = :id")
    suspend fun setStarred(id: Long, isStarred: Boolean)

    @Query("SELECT * FROM notification_records ORDER BY captureTime DESC")
    suspend fun getAllSync(): List<NotificationRecordEntity>
}
