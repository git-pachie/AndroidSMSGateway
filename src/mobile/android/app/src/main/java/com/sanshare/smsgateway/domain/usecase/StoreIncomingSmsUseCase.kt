package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.domain.model.IncomingSmsForwardStatus
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.sms.IncomingSmsFingerprint
import com.sanshare.smsgateway.sms.ParsedIncomingSms
import com.sanshare.smsgateway.sms.WebhookScheduler
import javax.inject.Inject
import javax.inject.Singleton

data class StoreIncomingSmsResult(
    val storedSmsId: Long? = null,
    val duplicateIgnored: Boolean = false,
)

@Singleton
class StoreIncomingSmsUseCase @Inject constructor(
    private val receivedSmsRepository: ReceivedSmsRepository,
    private val settingsRepository: SettingsRepository,
    private val fingerprintFactory: IncomingSmsFingerprint,
    private val webhookScheduler: WebhookScheduler,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(parsed: ParsedIncomingSms): StoreIncomingSmsResult {
        val fingerprint = fingerprintFactory.create(
            sender = parsed.sender,
            message = parsed.message,
            receivedAt = parsed.receivedAt,
            subscriptionId = parsed.subscriptionId,
        )
        val existing = receivedSmsRepository.findRecentByFingerprint(
            fingerprint = fingerprint,
            windowStart = parsed.receivedAt - IncomingSmsFingerprint.DUPLICATE_WINDOW_MILLIS,
        )
        if (existing != null) {
            logger.info("SMS", "Duplicate incoming SMS ignored from ${maskSender(parsed.sender)}")
            return StoreIncomingSmsResult(duplicateIgnored = true)
        }

        val settings = settingsRepository.getSettings()
        val initialStatus = if (settings.webhookEnabled) IncomingSmsForwardStatus.PENDING else IncomingSmsForwardStatus.DISABLED
        val smsId = receivedSmsRepository.insert(
            ReceivedSmsEntity(
                fromNumber = parsed.sender.trim(),
                message = parsed.message,
                receivedAt = parsed.receivedAt,
                subscriptionId = parsed.subscriptionId,
                simSlot = parsed.simSlot,
                forwardStatus = initialStatus,
                webhookResponseCode = null,
                webhookResponseBody = null,
                lastForwardAttemptAt = null,
                nextRetryAt = null,
                retryCount = 0,
                errorCode = if (settings.webhookEnabled) null else "WEBHOOK_DISABLED",
                errorMessage = if (settings.webhookEnabled) null else "Webhook forwarding is disabled",
                messageFingerprint = fingerprint,
            ),
        )
        if (settings.webhookEnabled) {
            webhookScheduler.scheduleReceivedSms(smsId)
        }
        logger.info("SMS", "Incoming SMS stored from ${maskSender(parsed.sender)} as id=$smsId")
        return StoreIncomingSmsResult(storedSmsId = smsId)
    }

    private fun maskSender(sender: String): String {
        val trimmed = sender.trim()
        if (trimmed.length <= 4) return trimmed
        return "${trimmed.take(2)}***${trimmed.takeLast(2)}"
    }
}
