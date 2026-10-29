package com.bc.notivault.di

import android.content.Context
import androidx.room.Room
import com.bc.notivault.data.db.NotiVaultDatabase
import com.bc.notivault.data.db.NotificationDao
import com.bc.notivault.data.preferences.PreferenceManager
import com.bc.notivault.data.repository.NotificationRepository
import com.bc.notivault.data.repository.NotificationRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NotiVaultDatabase {
        return Room.databaseBuilder(
            context,
            NotiVaultDatabase::class.java,
            NotiVaultDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideNotificationDao(database: NotiVaultDatabase): NotificationDao {
        return database.notificationDao()
    }

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao
    ): NotificationRepository {
        return NotificationRepositoryImpl(notificationDao)
    }

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @IoDispatcher dispatcher: CoroutineDispatcher
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
