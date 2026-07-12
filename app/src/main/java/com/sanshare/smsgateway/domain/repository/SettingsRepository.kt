package com.sanshare.smsgateway.domain.repository

import com.sanshare.smsgateway.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

data class GeneratedApiKey(
    val rawKey: String,
    val identifier: String,
)

interface SettingsRepository {
    fun observeSettings(): Flow<AppSettings?>
    suspend fun ensureInitialized(): GeneratedApiKey?
    suspend fun getSettings(): AppSettings
    suspend fun updateSettings(update: SettingsUpdate): AppSettings
    suspend fun updateServerPort(serverPort: Int): AppSettings
    suspend fun regenerateApiKey(): GeneratedApiKey
    suspend fun verifyApiKey(rawKey: String): Boolean
}

data class SettingsUpdate(
    val deviceId: String? = null,
    val webhookUrl: String? = null,
    val webhookEnabled: Boolean? = null,
    val allowedPrefixes: List<String>? = null,
    val rateLimitPerMinute: Int? = null,
    val dailySmsLimitEnabled: Boolean? = null,
    val dailySmsLimit: Int? = null,
    val maxRetryCount: Int? = null,
    val retryBaseDelaySeconds: Int? = null,
    val autoStartEnabled: Boolean? = null,
    val requireHttpsWebhook: Boolean? = null,
)
