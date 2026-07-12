package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.error.AppError
import com.sanshare.smsgateway.core.error.ErrorCode
import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.core.result.AppResult
import com.sanshare.smsgateway.core.security.RateLimiter
import com.sanshare.smsgateway.core.time.TimeProvider
import com.sanshare.smsgateway.core.validation.GatewayValidators
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.SmsSegmentEntity
import com.sanshare.smsgateway.domain.model.OutgoingSmsSegmentStatus
import com.sanshare.smsgateway.domain.model.OutgoingSmsStatus
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.sms.OutgoingSmsWebhookNotifier
import com.sanshare.smsgateway.sms.OutgoingSmsLifecycle
import com.sanshare.smsgateway.sms.SmsDispatcher
import com.sanshare.smsgateway.sms.SmsPermissionChecker
import javax.inject.Inject
import javax.inject.Singleton

data class SendSmsCommand(
    val to: String,
    val message: String,
    val clientReference: String? = null,
    val subscriptionId: Int? = null,
    val remoteAddress: String? = null,
)

data class AcceptedSms(
    val messageId: Long,
    val status: String,
    val to: String,
    val clientReference: String?,
    val createdAt: Long,
    val segmentCount: Int,
    val subscriptionId: Int?,
    val simSlot: Int?,
)

@Singleton
class SendSmsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sentSmsRepository: SentSmsRepository,
    private val dailySmsLimitUseCase: DailySmsLimitUseCase,
    private val smsDispatcher: SmsDispatcher,
    private val outgoingSmsWebhookNotifier: OutgoingSmsWebhookNotifier,
    private val statusUpdater: OutgoingSmsLifecycle,
    private val permissionChecker: SmsPermissionChecker,
    private val timeProvider: TimeProvider,
    private val logger: AppLogger,
) {
    private val rateLimiter = RateLimiter()

    suspend operator fun invoke(command: SendSmsCommand): AppResult<AcceptedSms> {
        if (!permissionChecker.canSendSms()) {
            logger.warning("SMS", "Send rejected due to missing SEND_SMS permission")
            return AppResult.Failure(AppError(ErrorCode.SMS_PERMISSION_DENIED, "SEND_SMS permission is required"))
        }

        val settings = settingsRepository.getSettings()
        val destination = GatewayValidators.destinationNumber(command.to)
        if (!destination.valid) return AppResult.Failure(AppError(ErrorCode.INVALID_REQUEST, destination.error ?: "Invalid destination"))
        val message = GatewayValidators.smsMessage(command.message)
        if (!message.valid) return AppResult.Failure(AppError(ErrorCode.INVALID_REQUEST, message.error ?: "Invalid message"))
        val clientReference = GatewayValidators.clientReference(command.clientReference)
        if (!clientReference.valid) return AppResult.Failure(AppError(ErrorCode.INVALID_REQUEST, clientReference.error ?: "Invalid client reference"))

        val normalizedDestination = destination.normalized.orEmpty()
        if (settings.allowedPrefixes.isNotEmpty() && settings.allowedPrefixes.none { normalizedDestination.startsWith(it) }) {
            return AppResult.Failure(AppError(ErrorCode.PREFIX_NOT_ALLOWED, "Destination prefix is not allowed"))
        }

        val globalLimit = rateLimiter.check(RateLimiter.GLOBAL_SEND_KEY, settings.rateLimitPerMinute)
        if (!globalLimit.allowed) {
            return AppResult.Failure(AppError(ErrorCode.RATE_LIMIT_EXCEEDED, "Global send rate limit exceeded", globalLimit.retryAfterSeconds.toString()))
        }
        command.remoteAddress?.takeIf { it.isNotBlank() }?.let { remote ->
            val decision = rateLimiter.check("send:remote:$remote", settings.rateLimitPerMinute)
            if (!decision.allowed) {
                return AppResult.Failure(AppError(ErrorCode.RATE_LIMIT_EXCEEDED, "Remote send rate limit exceeded", decision.retryAfterSeconds.toString()))
            }
        }

        val dailyLimit = dailySmsLimitUseCase.canAccept(timeProvider.nowMillis())
        if (!dailyLimit.allowed) {
            return AppResult.Failure(
                AppError(
                    ErrorCode.DAILY_SMS_LIMIT_EXCEEDED,
                    "Daily SMS limit exceeded",
                    "${dailyLimit.used}/${dailyLimit.limit}",
                ),
            )
        }

        val prepared = when (val result = smsDispatcher.prepare(message.normalized.orEmpty(), command.subscriptionId)) {
            is AppResult.Success -> result.value
            is AppResult.Failure -> return result
        }
        val now = timeProvider.nowMillis()
        val baseEntity = SentSmsEntity(
            toNumber = normalizedDestination,
            message = message.normalized.orEmpty(),
            clientReference = clientReference.normalized,
            status = OutgoingSmsStatus.PENDING,
            errorCode = null,
            errorMessage = null,
            createdAt = now,
            sendingAt = null,
            sentAt = null,
            deliveredAt = null,
            failedAt = null,
            retryCount = 0,
            segmentCount = prepared.segments.size,
            subscriptionId = prepared.subscriptionId,
            simSlot = prepared.simSlot,
        )
        val messageId = sentSmsRepository.insert(baseEntity)
        sentSmsRepository.saveSegments(
            messageId,
            prepared.segments.mapIndexed { index, _ ->
                SmsSegmentEntity(
                    sentSmsId = messageId,
                    segmentIndex = index,
                    totalSegments = prepared.segments.size,
                    sentStatus = OutgoingSmsSegmentStatus.PENDING,
                    deliveryStatus = OutgoingSmsSegmentStatus.PENDING,
                    sentResultCode = null,
                    deliveryResultCode = null,
                    updatedAt = now,
                )
            },
        )
        statusUpdater.markSending(messageId)
        return try {
            smsDispatcher.dispatch(messageId, normalizedDestination, prepared)
            sentSmsRepository.getById(messageId)?.let { outgoingSmsWebhookNotifier.notifyStatus(it) }
            logger.info(
                "SMS",
                "Outgoing SMS queued: id=$messageId to=${maskNumber(normalizedDestination)} segments=${prepared.segments.size}",
            )
            AppResult.Success(
                AcceptedSms(
                    messageId = messageId,
                    status = OutgoingSmsStatus.PENDING,
                    to = normalizedDestination,
                    clientReference = clientReference.normalized,
                    createdAt = now,
                    segmentCount = prepared.segments.size,
                    subscriptionId = prepared.subscriptionId,
                    simSlot = prepared.simSlot,
                ),
            )
        } catch (ex: SecurityException) {
            statusUpdater.markDispatchFailure(messageId, ErrorCode.SMS_PERMISSION_DENIED, "SMS permission is required")
            AppResult.Failure(AppError(ErrorCode.SMS_PERMISSION_DENIED, "SEND_SMS permission is required"))
        } catch (ex: Exception) {
            statusUpdater.markDispatchFailure(messageId, ErrorCode.SMS_NO_SERVICE, "SMS dispatch failed")
            logger.error("SMS", "Outgoing SMS dispatch threw unexpectedly", ex)
            AppResult.Failure(AppError(ErrorCode.SMS_NO_SERVICE, "SMS dispatch failed"))
        }
    }

    private fun maskNumber(value: String): String {
        if (value.length <= 4) return value
        return "${value.take(2)}***${value.takeLast(2)}"
    }
}
