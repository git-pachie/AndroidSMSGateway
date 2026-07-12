package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.error.ErrorCode
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.result.AppResult
import com.sanshare.smsgateway.core.time.TimeProvider
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity
import com.sanshare.smsgateway.domain.model.AppSettings
import com.sanshare.smsgateway.domain.repository.GeneratedApiKey
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SentSmsPage
import com.sanshare.smsgateway.domain.repository.SentSmsQuery
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.SettingsUpdate
import com.sanshare.smsgateway.sms.WebhookForwarder
import com.sanshare.smsgateway.sms.OutgoingSmsWebhookNotifier
import com.sanshare.smsgateway.sms.OutgoingSmsLifecycle
import com.sanshare.smsgateway.sms.PreparedSmsDispatch
import com.sanshare.smsgateway.sms.SmsDispatcher
import com.sanshare.smsgateway.sms.SmsPermissionChecker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SendSmsUseCaseTest {
    @Test
    fun sendsAcceptedMessageWhenValidationAndLimitsPass() = runTest {
        val settingsRepository = FakeSettingsRepository()
        val sentSmsRepository = FakeSentSmsRepository()
        val permissionChecker = FakePermissionChecker(true)
        val dispatcher = FakeSmsDispatcher(segments = listOf("hello", "world"))
        val lifecycle = FakeOutgoingSmsLifecycle()
        val useCase = SendSmsUseCase(
            settingsRepository = settingsRepository,
            sentSmsRepository = sentSmsRepository,
            dailySmsLimitUseCase = DailySmsLimitUseCase(settingsRepository, sentSmsRepository),
            smsDispatcher = dispatcher,
            outgoingSmsWebhookNotifier = buildWebhookNotifier(settingsRepository),
            statusUpdater = lifecycle,
            permissionChecker = permissionChecker,
            timeProvider = TimeProvider(),
            logger = NoopLogger(),
        )

        val result = useCase(
            SendSmsCommand(
                to = "+639171234567",
                message = "hello world",
                clientReference = "OTP-1",
            ),
        )

        assertTrue(result is AppResult.Success)
        val accepted = (result as AppResult.Success).value
        assertEquals(1L, accepted.messageId)
        assertEquals(1L, lifecycle.markSendingCalls.single())
        assertEquals(1L, dispatcher.dispatchedMessageIds.single())
        assertEquals(2, sentSmsRepository.savedSegments[1L]?.size)
    }

    @Test
    fun rejectsDestinationOutsideAllowedPrefixes() = runTest {
        val settingsRepository = FakeSettingsRepository(
            settings = defaultSettings.copy(allowedPrefixes = listOf("+63")),
        )
        val sentSmsRepository = FakeSentSmsRepository()
        val useCase = SendSmsUseCase(
            settingsRepository = settingsRepository,
            sentSmsRepository = sentSmsRepository,
            dailySmsLimitUseCase = DailySmsLimitUseCase(settingsRepository, sentSmsRepository),
            smsDispatcher = FakeSmsDispatcher(),
            outgoingSmsWebhookNotifier = buildWebhookNotifier(settingsRepository),
            statusUpdater = FakeOutgoingSmsLifecycle(),
            permissionChecker = FakePermissionChecker(true),
            timeProvider = TimeProvider(),
            logger = NoopLogger(),
        )

        val result = useCase(SendSmsCommand(to = "+15551234567", message = "hello"))

        assertTrue(result is AppResult.Failure)
        assertEquals(ErrorCode.PREFIX_NOT_ALLOWED, (result as AppResult.Failure).error.code)
    }

    @Test
    fun rejectsWhenDailyLimitAlreadyReached() = runTest {
        val settingsRepository = FakeSettingsRepository(
            settings = defaultSettings.copy(dailySmsLimit = 1, dailySmsLimitEnabled = true),
        )
        val sentSmsRepository = FakeSentSmsRepository(countAcceptedToday = 1)
        val useCase = SendSmsUseCase(
            settingsRepository = settingsRepository,
            sentSmsRepository = sentSmsRepository,
            dailySmsLimitUseCase = DailySmsLimitUseCase(settingsRepository, sentSmsRepository),
            smsDispatcher = FakeSmsDispatcher(),
            outgoingSmsWebhookNotifier = buildWebhookNotifier(settingsRepository),
            statusUpdater = FakeOutgoingSmsLifecycle(),
            permissionChecker = FakePermissionChecker(true),
            timeProvider = TimeProvider(),
            logger = NoopLogger(),
        )

        val result = useCase(SendSmsCommand(to = "+639171234567", message = "hello"))

        assertTrue(result is AppResult.Failure)
        assertEquals(ErrorCode.DAILY_SMS_LIMIT_EXCEEDED, (result as AppResult.Failure).error.code)
    }

    @Test
    fun enforcesGlobalRateLimit() = runTest {
        val settingsRepository = FakeSettingsRepository(
            settings = defaultSettings.copy(rateLimitPerMinute = 1),
        )
        val sentSmsRepository = FakeSentSmsRepository()
        val useCase = SendSmsUseCase(
            settingsRepository = settingsRepository,
            sentSmsRepository = sentSmsRepository,
            dailySmsLimitUseCase = DailySmsLimitUseCase(settingsRepository, sentSmsRepository),
            smsDispatcher = FakeSmsDispatcher(),
            outgoingSmsWebhookNotifier = buildWebhookNotifier(settingsRepository),
            statusUpdater = FakeOutgoingSmsLifecycle(),
            permissionChecker = FakePermissionChecker(true),
            timeProvider = TimeProvider(),
            logger = NoopLogger(),
        )

        val first = useCase(SendSmsCommand(to = "+639171234567", message = "hello"))
        val second = useCase(SendSmsCommand(to = "+639171234567", message = "hello again"))

        assertTrue(first is AppResult.Success)
        assertTrue(second is AppResult.Failure)
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, (second as AppResult.Failure).error.code)
    }

    private class FakePermissionChecker(
        private val allowed: Boolean,
    ) : SmsPermissionChecker {
        override fun canSendSms(): Boolean = allowed
    }

    private class FakeOutgoingSmsLifecycle : OutgoingSmsLifecycle {
        val markSendingCalls = mutableListOf<Long>()
        override suspend fun markSending(messageId: Long) {
            markSendingCalls += messageId
        }

        override suspend fun markDispatchFailure(messageId: Long, code: ErrorCode, message: String) = Unit
    }

    private class NoopLogger : AppLogger {
        override fun debug(category: String, message: String) = Unit
        override fun info(category: String, message: String) = Unit
        override fun warning(category: String, message: String) = Unit
        override fun error(category: String, message: String, throwable: Throwable?) = Unit
    }

    private class FakePreparedDispatch(
        override val subscriptionId: Int?,
        override val simSlot: Int?,
        override val segments: List<String>,
    ) : PreparedSmsDispatch

    private class FakeSmsDispatcher(
        private val segments: List<String> = listOf("hello"),
    ) : SmsDispatcher {
        val dispatchedMessageIds = mutableListOf<Long>()

        override fun prepare(message: String, requestedSubscriptionId: Int?): AppResult<PreparedSmsDispatch> {
            return AppResult.Success(FakePreparedDispatch(requestedSubscriptionId, 0, segments))
        }

        override fun dispatch(messageId: Long, destination: String, prepared: PreparedSmsDispatch) {
            dispatchedMessageIds += messageId
        }
    }

    private class FakeSentSmsRepository(
        private val countAcceptedToday: Int = 0,
    ) : SentSmsRepository {
        private var nextId = 1L
        private val items = linkedMapOf<Long, SentSmsEntity>()
        val savedSegments = mutableMapOf<Long, List<SmsSegmentEntity>>()

        override suspend fun insert(entity: SentSmsEntity): Long {
            val id = nextId++
            items[id] = entity.copy(id = id)
            return id
        }

        override suspend fun update(entity: SentSmsEntity) {
            items[entity.id] = entity
        }

        override suspend fun getById(id: Long): SentSmsEntity? = items[id]

        override fun observeById(id: Long): Flow<SentSmsEntity?> = flowOf(items[id])

        override suspend fun getLatestByClientReference(clientReference: String): SentSmsEntity? = null

        override suspend fun saveSegments(messageId: Long, segments: List<SmsSegmentEntity>) {
            savedSegments[messageId] = segments
        }

        override suspend fun getSegments(messageId: Long): List<SmsSegmentEntity> = savedSegments[messageId].orEmpty()

        override suspend fun query(query: SentSmsQuery): SentSmsPage = SentSmsPage(items.values.toList(), items.size)

        override suspend fun countAcceptedTodayUtc(nowMillis: Long): Int = countAcceptedToday

        override fun countByStatus(status: String): Flow<Int> = flowOf(items.values.count { it.status == status })
    }

    private class FakeSettingsRepository(
        private val settings: AppSettings = defaultSettings,
    ) : SettingsRepository {
        override fun observeSettings(): Flow<AppSettings?> = flowOf(settings)
        override suspend fun ensureInitialized(): GeneratedApiKey? = null
        override suspend fun getSettings(): AppSettings = settings
        override suspend fun getWebhookSecret(): String? = null
        override suspend fun updateSettings(update: SettingsUpdate): AppSettings = settings
        override suspend fun updateServerPort(serverPort: Int): AppSettings = settings.copy(serverPort = serverPort)
        override suspend fun setServerEnabled(enabled: Boolean): AppSettings = settings.copy(serverEnabled = enabled)
        override suspend fun regenerateApiKey(): GeneratedApiKey = GeneratedApiKey("raw", "id")
        override suspend fun verifyApiKey(rawKey: String): Boolean = true
    }

    companion object {
        private fun buildWebhookNotifier(settingsRepository: SettingsRepository): OutgoingSmsWebhookNotifier {
            val logger = NoopLogger()
            return OutgoingSmsWebhookNotifier(
                settingsRepository = settingsRepository,
                webhookForwarder = WebhookForwarder(settingsRepository, logger),
                logger = logger,
            )
        }

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
            serverEnabled = true,
        )
    }
}
