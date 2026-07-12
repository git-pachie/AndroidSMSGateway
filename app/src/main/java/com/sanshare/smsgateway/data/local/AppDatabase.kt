package com.sanshare.smsgateway.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.sanshare.smsgateway.data.local.dao.ReceivedSmsDao
import com.sanshare.smsgateway.data.local.dao.RequestAuditLogDao
import com.sanshare.smsgateway.data.local.dao.SentSmsDao
import com.sanshare.smsgateway.data.local.dao.SettingsDao
import com.sanshare.smsgateway.data.local.dao.SmsSegmentDao
import com.sanshare.smsgateway.data.local.dao.SystemLogDao
import com.sanshare.smsgateway.data.local.dao.WebhookAttemptDao
import com.sanshare.smsgateway.data.local.entity.AppSettingsEntity
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity

@Database(
    entities = [
        SentSmsEntity::class,
        SmsSegmentEntity::class,
        ReceivedSmsEntity::class,
        AppSettingsEntity::class,
        SystemLogEntity::class,
        WebhookAttemptEntity::class,
        RequestAuditLogEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun sentSmsDao(): SentSmsDao
    abstract fun smsSegmentDao(): SmsSegmentDao
    abstract fun receivedSmsDao(): ReceivedSmsDao
    abstract fun settingsDao(): SettingsDao
    abstract fun systemLogDao(): SystemLogDao
    abstract fun webhookAttemptDao(): WebhookAttemptDao
    abstract fun requestAuditLogDao(): RequestAuditLogDao
}
