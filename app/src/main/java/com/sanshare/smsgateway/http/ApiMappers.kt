package com.sanshare.smsgateway.http

import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity
import com.sanshare.smsgateway.data.local.entity.WebhookAttemptEntity
import com.sanshare.smsgateway.domain.usecase.AcceptedSms
import com.sanshare.smsgateway.sms.WebhookDispatchResult
import com.sanshare.smsgateway.sms.WebhookRetryPolicy
import com.sanshare.smsgateway.domain.model.AppSettings
import java.time.Instant

fun AppSettings.toSafeResponse(): SafeSettingsResponse {
    return SafeSettingsResponse(
        deviceId = deviceId,
        serverPort = serverPort,
        webhookEnabled = webhookEnabled,
        webhookUrl = webhookUrl,
        webhookSecretConfigured = webhookSecretConfigured,
        apiKeyConfigured = apiKeyConfigured,
        apiKeyIdentifier = apiKeyIdentifier,
        allowedPrefixes = allowedPrefixes,
        rateLimitPerMinute = rateLimitPerMinute,
        dailySmsLimitEnabled = dailySmsLimitEnabled,
        dailySmsLimit = dailySmsLimit,
        maxRetryCount = maxRetryCount,
        retryBaseDelaySeconds = retryBaseDelaySeconds,
        effectiveRetryBaseDelaySeconds = WebhookRetryPolicy.effectiveBaseDelaySeconds(retryBaseDelaySeconds),
        autoStartEnabled = autoStartEnabled,
        requireHttpsWebhook = requireHttpsWebhook,
    )
}

fun SystemLogEntity.toDto(): SystemLogDto {
    return SystemLogDto(id, level, category, eventCode, message, details, createdAt)
}

fun RequestAuditLogEntity.toDto(): AuditLogDto {
    return AuditLogDto(id, method, path, remoteAddress, responseCode, durationMs, authenticated, createdAt)
}

fun AcceptedSms.toResponse(): AcceptedSmsResponse {
    return AcceptedSmsResponse(
        messageId = messageId,
        status = status,
        to = to,
        clientReference = clientReference,
        createdAt = createdAt.toIsoInstant(),
    )
}

fun SentSmsEntity.toStatusResponse(): SentSmsStatusResponse {
    return SentSmsStatusResponse(
        messageId = id,
        to = toNumber,
        message = message,
        status = status,
        createdAt = createdAt.toIsoInstant(),
        sendingAt = sendingAt?.toIsoInstant(),
        sentAt = sentAt?.toIsoInstant(),
        deliveredAt = deliveredAt?.toIsoInstant(),
        failedAt = failedAt?.toIsoInstant(),
        clientReference = clientReference,
        segmentCount = segmentCount,
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        errorCode = errorCode,
        errorMessage = errorMessage,
    )
}

fun SentSmsEntity.toListItemResponse(): SentSmsListItemResponse {
    return SentSmsListItemResponse(
        messageId = id,
        to = toNumber,
        status = status,
        createdAt = createdAt.toIsoInstant(),
        sentAt = sentAt?.toIsoInstant(),
        deliveredAt = deliveredAt?.toIsoInstant(),
        failedAt = failedAt?.toIsoInstant(),
        clientReference = clientReference,
        segmentCount = segmentCount,
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        errorCode = errorCode,
    )
}

fun ReceivedSmsEntity.toListItemResponse(): ReceivedSmsListItemResponse {
    return ReceivedSmsListItemResponse(
        smsId = id,
        from = fromNumber,
        message = message,
        receivedAt = receivedAt.toIsoInstant(),
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        forwardStatus = forwardStatus,
        retryCount = retryCount,
        lastForwardAttemptAt = lastForwardAttemptAt?.toIsoInstant(),
        nextRetryAt = nextRetryAt?.toIsoInstant(),
        webhookResponseCode = webhookResponseCode,
        errorCode = errorCode,
        errorMessage = errorMessage,
    )
}

fun ReceivedSmsEntity.toDetailResponse(attempts: List<WebhookAttemptEntity>): ReceivedSmsDetailResponse {
    return ReceivedSmsDetailResponse(
        smsId = id,
        from = fromNumber,
        message = message,
        receivedAt = receivedAt.toIsoInstant(),
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        forwardStatus = forwardStatus,
        retryCount = retryCount,
        lastForwardAttemptAt = lastForwardAttemptAt?.toIsoInstant(),
        nextRetryAt = nextRetryAt?.toIsoInstant(),
        webhookResponseCode = webhookResponseCode,
        webhookResponseBody = webhookResponseBody,
        errorCode = errorCode,
        errorMessage = errorMessage,
        attempts = attempts.map { it.toSummaryResponse() },
    )
}

fun WebhookAttemptEntity.toResponse(): WebhookAttemptResponse {
    return WebhookAttemptResponse(
        attemptId = id,
        smsId = receivedSmsId,
        attemptNumber = attemptNumber,
        requestUrlSummary = requestUrlSummary,
        responseCode = responseCode,
        responseBodySummary = responseBodySummary,
        durationMs = durationMs,
        success = success,
        errorCode = errorCode,
        errorMessage = errorMessage,
        attemptedAt = attemptedAt.toIsoInstant(),
    )
}

fun WebhookAttemptEntity.toSummaryResponse(): WebhookAttemptSummaryResponse {
    return WebhookAttemptSummaryResponse(
        attemptId = id,
        attemptNumber = attemptNumber,
        responseCode = responseCode,
        success = success,
        durationMs = durationMs,
        errorCode = errorCode,
        attemptedAt = attemptedAt.toIsoInstant(),
    )
}

fun WebhookDispatchResult.toWebhookTestResponse(): WebhookTestResponse {
    return WebhookTestResponse(
        success = success,
        responseCode = responseCode,
        durationMs = durationMs,
        responseBodySummary = responseBodySummary,
        errorCode = errorCode,
        errorMessage = errorMessage,
    )
}

private fun Long.toIsoInstant(): String = Instant.ofEpochMilli(this).toString()
