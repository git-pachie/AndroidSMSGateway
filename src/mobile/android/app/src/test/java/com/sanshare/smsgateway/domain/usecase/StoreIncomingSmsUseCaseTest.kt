package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.domain.model.AppSettings
import com.sanshare.smsgateway.domain.repository.ReceivedSmsPage
import com.sanshare.smsgateway.domain.repository.ReceivedSmsQuery
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.GeneratedApiKey
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SettingsUpdate
import com.sanshare.smsgateway.sms.IncomingSmsFingerprint
import com.sanshare.smsgateway.sms.ParsedIncomingSms
import com.sanshare.smsgateway.sms.WebhookScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreIncomingSmsUseCaseTest {
    @Test
    fun storesIncomingSmsAndPreservesAlphanumericSender() = runTest {
        val repo = FakeReceivedSmsRepository()
        val useCase = StoreIncomingSmsUseCase(
            receivedSmsRepository = repo,
            settingsRepository = FakeSettingsRepository(),
            fingerprintFactory = IncomingSmsFingerprint(),
            webhookScheduler = FakeWebhookScheduler(),
            logger = NoopLogger(),
        )

        val result = useCase(
            ParsedIncomingSms(
                sender = "ACMEBANK",
                message = "OTP 123456",
                receivedAt = 1_000L,
                subscriptionId = 1,
                simSlot = 0,
            ),
        )

        assertEquals(1L, result.storedSmsId)
        assertEquals("ACMEBANK", repo.items.single().fromNumber)
    }

    @Test
    fun ignoresDuplicateFingerprintInsideWindow() = runTest {
        val repo = FakeReceivedSmsRepository()
        val fingerprint = IncomingSmsFingerprint()
        val sender = "ACMEBANK"
        val message = "OTP 123456"
        val ts = 1_000L
        repo.items += ReceivedSmsEntity(
            id = 1L,
            fromNumber = sender,
            message = message,
            receivedAt = ts,
            subscriptionId = 1,
            simSlot = 0,
            forwardStatus = "PENDING",
            webhookResponseCode = null,
            webhookResponseBody = null,
            lastForwardAttemptAt = null,
            nextRetryAt = null,
            retryCount = 0,
            errorCode = null,
            errorMessage = null,
            messageFingerprint = fingerprint.create(sender, message, ts, 1),
        )
        val useCase = StoreIncomingSmsUseCase(
            receivedSmsRepository = repo,
            settingsRepository = FakeSettingsRepository(),
            fingerprintFactory = fingerprint,
            webhookScheduler = FakeWebhookScheduler(),
            logger = NoopLogger(),
        )

        val result = useCase(
            ParsedIncomingSms(
                sender = sender,
                message = message,
                receivedAt = ts + 500L,
                subscriptionId = 1,
                simSlot = 0,
            ),
        )

        assertTrue(result.duplicateIgnored)
        assertEquals(1, repo.items.size)
    }

    @Test
    fun storesDisabledStateWhenWebhookDisabled() = runTest {
        val repo = FakeReceivedSmsRepository()
        val useCase = StoreIncomingSmsUseCase(
            receivedSmsRepository = repo,
            settingsRepository = FakeSettingsRepository(webhookEnabled = false),
            fingerprintFactory = IncomingSmsFingerprint(),
            webhookScheduler = FakeWebhookScheduler(),
            logger = NoopLogger(),
        )

        useCase(
            ParsedIncomingSms(
                sender = "BANK",
                message = "OTP 654321",
                receivedAt = 1_000L,
                subscriptionId = null,
                simSlot = null,
            ),
        )

        assertEquals("DISABLED", repo.items.single().forwardStatus)
        assertEquals("WEBHOOK_DISABLED", repo.items.single().errorCode)
    }

    private class FakeWebhookScheduler : WebhookScheduler {
        override suspend fun scheduleReceivedSms(receivedSmsId: Long, replaceExisting: Boolean): Boolean = true
        override fun observeScheduled(receivedSmsId: Long): Flow<Boolean> = flowOf(false)
    }

    private class FakeSettingsRepository(
        webhookEnabled: Boolean = true,
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

        companion object {
            private fun settings(webhookEnabled: Boolean) = AppSettings(
                deviceId = "device",
                serverPort = 8080,
                apiKeyConfigured = true,
                apiKeyIdentifier = "id",
                webhookUrl = "https://example.com/webhook",
                webhookEnabled = webhookEnabled,
                webhookSecretConfigured = false,
                maxRetryCount = 5,
                retryBaseDelaySeconds = 30,
                allowedPrefixes = emptyList(),
                dailySmsLimitEnabled = true,
                dailySmsLimit = 500,
                rateLimitPerMinute = 60,
                autoStartEnabled = false,
                requireHttpsWebhook = true,
                serverEnabled = true,
            )
        }

        private val settings = settings(webhookEnabled)
    }

    private class NoopLogger : AppLogger {
        override fun debug(category: String, message: String) = Unit
        override fun info(category: String, message: String) = Unit
        override fun warning(category: String, message: String) = Unit
        override fun error(category: String, message: String, throwable: Throwable?) = Unit
    }

    private class FakeReceivedSmsRepository : ReceivedSmsRepository {
        val items = mutableListOf<ReceivedSmsEntity>()

        override suspend fun insert(entity: ReceivedSmsEntity): Long {
            val id = (items.maxOfOrNull { it.id } ?: 0L) + 1L
            items += entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: ReceivedSmsEntity) = Unit

        override suspend fun getById(id: Long): ReceivedSmsEntity? = items.firstOrNull { it.id == id }

        override fun observeById(id: Long): Flow<ReceivedSmsEntity?> = flowOf(getByIdBlocking(id))

        override suspend fun findRecentByFingerprint(fingerprint: String, windowStart: Long): ReceivedSmsEntity? {
            return items.firstOrNull { it.messageFingerprint == fingerprint && it.receivedAt >= windowStart }
        }

        override suspend fun query(query: ReceivedSmsQuery): ReceivedSmsPage = ReceivedSmsPage(items.toList(), items.size)

        override fun observeQuery(query: ReceivedSmsQuery): Flow<List<ReceivedSmsEntity>> = flowOf(items.toList())

        override fun countByForwardStatus(status: String): Flow<Int> = flowOf(items.count { it.forwardStatus == status })

        override suspend fun countReceivedTodayUtc(nowMillis: Long): Int = items.size

        private fun getByIdBlocking(id: Long): ReceivedSmsEntity? = items.firstOrNull { it.id == id }
    }
}
