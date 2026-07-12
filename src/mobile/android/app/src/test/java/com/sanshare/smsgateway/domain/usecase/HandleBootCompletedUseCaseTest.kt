package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.domain.model.AppSettings
import com.sanshare.smsgateway.domain.repository.GeneratedApiKey
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SettingsUpdate
import com.sanshare.smsgateway.service.GatewayServiceStarter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HandleBootCompletedUseCaseTest {
    @Test
    fun startsGatewayWhenAutoStartEnabled() = runTest {
        val starter = FakeGatewayServiceStarter()
        val useCase = HandleBootCompletedUseCase(
            settingsRepository = FakeSettingsRepository(defaultSettings.copy(autoStartEnabled = true)),
            gatewayServiceStarter = starter,
            logger = NoopLogger(),
        )

        val started = useCase()

        assertTrue(started)
        assertTrue(starter.started)
    }

    @Test
    fun exitsWhenAutoStartDisabled() = runTest {
        val starter = FakeGatewayServiceStarter()
        val useCase = HandleBootCompletedUseCase(
            settingsRepository = FakeSettingsRepository(defaultSettings.copy(autoStartEnabled = false)),
            gatewayServiceStarter = starter,
            logger = NoopLogger(),
        )

        val started = useCase()

        assertFalse(started)
        assertFalse(starter.started)
    }

    private class FakeGatewayServiceStarter : GatewayServiceStarter {
        var started = false
        override fun start() {
            started = true
        }
    }

    private class FakeSettingsRepository(
        private val settings: AppSettings,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings?> = flowOf(settings)
        override suspend fun ensureInitialized(): GeneratedApiKey? = null
        override suspend fun getSettings(): AppSettings = settings
        override suspend fun getWebhookSecret(): String? = null
        override suspend fun updateSettings(update: SettingsUpdate): AppSettings = settings
        override suspend fun updateServerPort(serverPort: Int): AppSettings = settings
        override suspend fun setServerEnabled(enabled: Boolean): AppSettings = settings
        override suspend fun regenerateApiKey(): GeneratedApiKey = GeneratedApiKey("raw", "id")
        override suspend fun verifyApiKey(rawKey: String): Boolean = true
    }

    private class NoopLogger : AppLogger {
        override fun debug(category: String, message: String) = Unit
        override fun info(category: String, message: String) = Unit
        override fun warning(category: String, message: String) = Unit
        override fun error(category: String, message: String, throwable: Throwable?) = Unit
    }

    companion object {
        private val defaultSettings = AppSettings(
            deviceId = "android-gateway-01",
            serverPort = 8080,
            apiKeyConfigured = true,
            apiKeyIdentifier = "id",
            webhookUrl = null,
            webhookEnabled = false,
            webhookSecretConfigured = false,
            maxRetryCount = 5,
            retryBaseDelaySeconds = 30,
            allowedPrefixes = emptyList(),
            dailySmsLimitEnabled = true,
            dailySmsLimit = 500,
            rateLimitPerMinute = 60,
            autoStartEnabled = false,
            requireHttpsWebhook = true,
            serverEnabled = false,
        )
    }
}
