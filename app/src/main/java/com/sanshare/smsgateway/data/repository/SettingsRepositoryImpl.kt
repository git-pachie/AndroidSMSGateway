package com.sanshare.smsgateway.data.repository

import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.core.security.ApiKeyHasher
import com.sanshare.smsgateway.core.validation.GatewayValidators
import com.sanshare.smsgateway.data.local.dao.SettingsDao
import com.sanshare.smsgateway.data.local.entity.AppSettingsEntity
import com.sanshare.smsgateway.domain.model.AppSettings
import com.sanshare.smsgateway.domain.repository.GeneratedApiKey
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SettingsUpdate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDao: SettingsDao,
) : SettingsRepository {
    private val apiKeyHasher = ApiKeyHasher()

    override fun observeSettings(): Flow<AppSettings?> {
        return settingsDao.observeById().map { it?.toDomain() }
    }

    override suspend fun ensureInitialized(): GeneratedApiKey? {
        if (settingsDao.getById() != null) return null
        val rawKey = apiKeyHasher.generateRawKey()
        val hash = apiKeyHasher.hash(rawKey)
        val now = System.currentTimeMillis()
        val inserted = settingsDao.insertIgnore(
            AppSettingsEntity(
                deviceId = AppConstants.DEFAULT_DEVICE_ID,
                serverPort = AppConstants.DEFAULT_SERVER_PORT,
                apiKeyHash = hash.encoded,
                apiKeyIdentifier = hash.identifier,
                webhookUrl = null,
                webhookEnabled = false,
                webhookSecretEncrypted = null,
                maxRetryCount = 5,
                retryBaseDelaySeconds = 30,
                allowedPrefixes = null,
                dailySmsLimitEnabled = true,
                dailySmsLimit = 500,
                rateLimitPerMinute = 60,
                autoStartEnabled = false,
                requireHttpsWebhook = true,
                serverEnabled = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return if (inserted >= 0) GeneratedApiKey(rawKey, hash.identifier) else null
    }

    override suspend fun getSettings(): AppSettings {
        ensureInitialized()
        return requireNotNull(settingsDao.getById()).toDomain()
    }

    override suspend fun updateSettings(update: SettingsUpdate): AppSettings {
        val current = requireNotNull(settingsDao.getById())
        val updated = current.copy(
            deviceId = update.deviceId?.trim()?.takeIf { it.isNotBlank() } ?: current.deviceId,
            webhookUrl = update.webhookUrl ?: current.webhookUrl,
            webhookEnabled = update.webhookEnabled ?: current.webhookEnabled,
            allowedPrefixes = update.allowedPrefixes?.joinToString(",") ?: current.allowedPrefixes,
            rateLimitPerMinute = update.rateLimitPerMinute ?: current.rateLimitPerMinute,
            dailySmsLimitEnabled = update.dailySmsLimitEnabled ?: current.dailySmsLimitEnabled,
            dailySmsLimit = update.dailySmsLimit ?: current.dailySmsLimit,
            maxRetryCount = update.maxRetryCount ?: current.maxRetryCount,
            retryBaseDelaySeconds = update.retryBaseDelaySeconds ?: current.retryBaseDelaySeconds,
            autoStartEnabled = update.autoStartEnabled ?: current.autoStartEnabled,
            requireHttpsWebhook = update.requireHttpsWebhook ?: current.requireHttpsWebhook,
            updatedAt = System.currentTimeMillis(),
        )
        settingsDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun updateServerPort(serverPort: Int): AppSettings {
        val current = requireNotNull(settingsDao.getById())
        val updated = current.copy(serverPort = serverPort, updatedAt = System.currentTimeMillis())
        settingsDao.update(updated)
        return updated.toDomain()
    }

    override suspend fun regenerateApiKey(): GeneratedApiKey {
        val current = requireNotNull(settingsDao.getById())
        val rawKey = apiKeyHasher.generateRawKey()
        val hash = apiKeyHasher.hash(rawKey)
        settingsDao.update(
            current.copy(
                apiKeyHash = hash.encoded,
                apiKeyIdentifier = hash.identifier,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        return GeneratedApiKey(rawKey, hash.identifier)
    }

    override suspend fun verifyApiKey(rawKey: String): Boolean {
        val current = settingsDao.getById() ?: return false
        return apiKeyHasher.verify(rawKey, current.apiKeyHash)
    }

    private fun AppSettingsEntity.toDomain(): AppSettings {
        return AppSettings(
            deviceId = deviceId,
            serverPort = serverPort,
            apiKeyConfigured = apiKeyHash.isNotBlank(),
            apiKeyIdentifier = apiKeyIdentifier,
            webhookUrl = webhookUrl,
            webhookEnabled = webhookEnabled,
            webhookSecretConfigured = !webhookSecretEncrypted.isNullOrBlank(),
            maxRetryCount = maxRetryCount,
            retryBaseDelaySeconds = retryBaseDelaySeconds,
            allowedPrefixes = GatewayValidators.allowedPrefixes(allowedPrefixes),
            dailySmsLimitEnabled = dailySmsLimitEnabled,
            dailySmsLimit = dailySmsLimit,
            rateLimitPerMinute = rateLimitPerMinute,
            autoStartEnabled = autoStartEnabled,
            requireHttpsWebhook = requireHttpsWebhook,
            serverEnabled = serverEnabled,
        )
    }
}
