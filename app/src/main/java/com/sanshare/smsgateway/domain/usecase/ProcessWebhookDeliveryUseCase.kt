package com.sanshare.smsgateway.domain.usecase

import androidx.room.withTransaction
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.data.local.AppDatabase
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity
import com.sanshare.smsgateway.domain.model.IncomingSmsForwardStatus
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import com.sanshare.smsgateway.sms.WebhookDispatchRequest
import com.sanshare.smsgateway.sms.WebhookForwarder
import com.sanshare.smsgateway.sms.WebhookRetryPolicy
import com.sanshare.smsgateway.sms.toWebhookPayload
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

enum class WebhookExecutionState {
    SUCCESS,
    RETRY,
    FAILED,
    MISSING_SMS,
}

data class WebhookExecutionResult(
    val state: WebhookExecutionState,
    val errorCode: String? = null,
)

@Singleton
class ProcessWebhookDeliveryUseCase @Inject constructor(
    private val appDatabase: AppDatabase,
    private val receivedSmsRepository: ReceivedSmsRepository,
    private val webhookAttemptRepository: WebhookAttemptRepository,
    private val settingsRepository: SettingsRepository,
    private val webhookForwarder: WebhookForwarder,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(receivedSmsId: Long): WebhookExecutionResult {
        val sms = receivedSmsRepository.getById(receivedSmsId) ?: return WebhookExecutionResult(WebhookExecutionState.MISSING_SMS)
        val settings = settingsRepository.getSettings()
        val dispatch = webhookForwarder.forward(
            WebhookDispatchRequest(
                payload = sms.toWebhookPayload(settings.deviceId),
                requestId = UUID.randomUUID().toString(),
            ),
        )
        val now = System.currentTimeMillis()
        val attemptNumber = sms.retryCount + 1

        return appDatabase.withTransaction {
            if (dispatch.errorCode == "WEBHOOK_DISABLED") {
                receivedSmsRepository.update(
                    sms.copy(
                        forwardStatus = IncomingSmsForwardStatus.DISABLED,
                        lastForwardAttemptAt = null,
                        nextRetryAt = null,
                        errorCode = dispatch.errorCode,
                        errorMessage = dispatch.errorMessage,
                    ),
                )
                return@withTransaction WebhookExecutionResult(WebhookExecutionState.FAILED, dispatch.errorCode)
            }

            webhookAttemptRepository.insert(
                WebhookAttemptEntity(
                    receivedSmsId = sms.id,
                    attemptNumber = attemptNumber,
                    requestUrlSummary = dispatch.requestUrlSummary ?: "not-configured",
                    responseCode = dispatch.responseCode,
                    responseBodySummary = dispatch.responseBodySummary,
                    durationMs = dispatch.durationMs,
                    success = dispatch.success,
                    errorCode = dispatch.errorCode,
                    errorMessage = dispatch.errorMessage,
                    attemptedAt = now,
                ),
            )

            if (dispatch.success) {
                receivedSmsRepository.update(
                    sms.copy(
                        forwardStatus = IncomingSmsForwardStatus.FORWARDED,
                        webhookResponseCode = dispatch.responseCode,
                        webhookResponseBody = dispatch.responseBodySummary,
                        lastForwardAttemptAt = now,
                        nextRetryAt = null,
                        errorCode = null,
                        errorMessage = null,
                    ),
                )
                logger.info("WEBHOOK", "Webhook forwarded inbox smsId=${sms.id}")
                return@withTransaction WebhookExecutionResult(WebhookExecutionState.SUCCESS)
            }

            val newRetryCount = sms.retryCount + 1
            val hasRetriesRemaining = dispatch.retryable && newRetryCount <= settings.maxRetryCount
            val nextRetryAt = if (hasRetriesRemaining) {
                now + WebhookRetryPolicy.nextRetryDelayMillis(settings.retryBaseDelaySeconds, newRetryCount)
            } else {
                null
            }

            receivedSmsRepository.update(
                sms.copy(
                    forwardStatus = if (hasRetriesRemaining) IncomingSmsForwardStatus.PENDING else IncomingSmsForwardStatus.FAILED,
                    webhookResponseCode = dispatch.responseCode,
                    webhookResponseBody = dispatch.responseBodySummary,
                    lastForwardAttemptAt = now,
                    nextRetryAt = nextRetryAt,
                    retryCount = newRetryCount,
                    errorCode = if (!hasRetriesRemaining && dispatch.retryable) "WEBHOOK_RETRIES_EXHAUSTED" else dispatch.errorCode,
                    errorMessage = dispatch.errorMessage,
                ),
            )
            if (hasRetriesRemaining) {
                logger.warning("WEBHOOK", "Webhook delivery will retry for smsId=${sms.id}")
                WebhookExecutionResult(WebhookExecutionState.RETRY, dispatch.errorCode)
            } else {
                logger.warning("WEBHOOK", "Webhook delivery failed permanently for smsId=${sms.id}")
                WebhookExecutionResult(WebhookExecutionState.FAILED, dispatch.errorCode)
            }
        }
    }
}
