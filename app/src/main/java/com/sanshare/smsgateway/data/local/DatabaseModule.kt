package com.sanshare.smsgateway.data.local

import android.content.Context
import androidx.room.Room
import com.sanshare.smsgateway.data.local.dao.ReceivedSmsDao
import com.sanshare.smsgateway.data.local.dao.RequestAuditLogDao
import com.sanshare.smsgateway.data.local.dao.SentSmsDao
import com.sanshare.smsgateway.data.local.dao.SettingsDao
import com.sanshare.smsgateway.data.local.dao.SystemLogDao
import com.sanshare.smsgateway.data.local.dao.WebhookAttemptDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "sms_gateway.db")
            .build()
    }

    @Provides fun provideSentSmsDao(database: AppDatabase): SentSmsDao = database.sentSmsDao()
    @Provides fun provideReceivedSmsDao(database: AppDatabase): ReceivedSmsDao = database.receivedSmsDao()
    @Provides fun provideSettingsDao(database: AppDatabase): SettingsDao = database.settingsDao()
    @Provides fun provideSystemLogDao(database: AppDatabase): SystemLogDao = database.systemLogDao()
    @Provides fun provideWebhookAttemptDao(database: AppDatabase): WebhookAttemptDao = database.webhookAttemptDao()
    @Provides fun provideRequestAuditLogDao(database: AppDatabase): RequestAuditLogDao = database.requestAuditLogDao()
}
