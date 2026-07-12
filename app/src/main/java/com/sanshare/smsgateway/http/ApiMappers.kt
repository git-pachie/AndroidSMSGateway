package com.sanshare.smsgateway.http

import com.sanshare.smsgateway.data.local.entity.RequestAuditLogEntity
import com.sanshare.smsgateway.data.local.entity.SystemLogEntity
import com.sanshare.smsgateway.domain.model.AppSettings

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
