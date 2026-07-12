package com.sanshare.smsgateway.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = STABLE_ID,
    val deviceId: String,
    val serverPort: Int,
    val apiKeyHash: String,
    val apiKeyIdentifier: String?,
    val webhookUrl: String?,
    val webhookEnabled: Boolean,
    val webhookSecretEncrypted: String?,
    val maxRetryCount: Int,
    val retryBaseDelaySeconds: Int,
    val allowedPrefixes: String?,
    val dailySmsLimitEnabled: Boolean,
    val dailySmsLimit: Int,
    val rateLimitPerMinute: Int,
    val autoStartEnabled: Boolean,
    val requireHttpsWebhook: Boolean,
    val serverEnabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
) {
    companion object {
        const val STABLE_ID = 1
    }
}
