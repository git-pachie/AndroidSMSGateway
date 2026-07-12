package com.sanshare.smsgateway.http

import kotlinx.serialization.Serializable

@Serializable
data class ApiSuccess<T>(
    val success: Boolean = true,
    val data: T,
)

@Serializable
data class ApiErrorEnvelope(
    val success: Boolean = false,
    val error: ApiErrorBody,
    val requestId: String,
    val timestamp: String,
)

@Serializable
data class ApiErrorBody(
    val code: String,
    val message: String,
    val details: String? = null,
)

@Serializable
data class HealthResponse(
    val status: String,
    val deviceId: String,
    val serverTime: String,
    val version: String,
    val smsPermission: Boolean,
    val receiveSmsPermission: Boolean,
    val notificationPermission: Boolean,
    val batteryOptimizationDisabled: Boolean,
    val simAvailable: Boolean? = null,
    val webhookEnabled: Boolean,
    val pendingWebhookCount: Int,
)

@Serializable
data class SafeSettingsResponse(
    val deviceId: String,
    val serverPort: Int,
    val webhookEnabled: Boolean,
    val webhookUrl: String?,
    val webhookSecretConfigured: Boolean,
    val apiKeyConfigured: Boolean,
    val apiKeyIdentifier: String?,
    val allowedPrefixes: List<String>,
    val rateLimitPerMinute: Int,
    val dailySmsLimitEnabled: Boolean,
    val dailySmsLimit: Int,
    val maxRetryCount: Int,
    val retryBaseDelaySeconds: Int,
    val effectiveRetryBaseDelaySeconds: Int,
    val autoStartEnabled: Boolean,
    val requireHttpsWebhook: Boolean,
)

@Serializable
data class SettingsUpdateRequest(
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

@Serializable
data class WebhookSettingsRequest(
    val webhookUrl: String? = null,
    val enabled: Boolean? = null,
    val webhookSecret: String? = null,
    val clearWebhookSecret: Boolean = false,
    val maxRetryCount: Int? = null,
)

@Serializable
data class ServerSettingsRequest(
    val serverPort: Int,
    val restartNow: Boolean = false,
)

@Serializable
data class RestartRequiredResponse(
    val settings: SafeSettingsResponse,
    val restartRequired: Boolean,
)

@Serializable
data class PagedResponse<T>(
    val items: List<T>,
    val limit: Int,
    val offset: Int,
    val returned: Int,
    val total: Int,
)

@Serializable
data class SystemLogDto(
    val id: Long,
    val level: String,
    val category: String,
    val eventCode: String?,
    val message: String,
    val details: String?,
    val createdAt: Long,
)

@Serializable
data class AuditLogDto(
    val id: Long,
    val method: String,
    val path: String,
    val remoteAddress: String?,
    val responseCode: Int,
    val durationMs: Long,
    val authenticated: Boolean,
    val createdAt: Long,
)

@Serializable
data class SendSmsRequest(
    val to: String,
    val message: String,
    val clientReference: String? = null,
    val subscriptionId: Int? = null,
)

@Serializable
data class AcceptedSmsResponse(
    val messageId: Long,
    val status: String,
    val to: String,
    val clientReference: String? = null,
    val createdAt: String,
)

@Serializable
data class SentSmsStatusResponse(
    val messageId: Long,
    val to: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val sendingAt: String?,
    val sentAt: String?,
    val deliveredAt: String?,
    val failedAt: String?,
    val clientReference: String?,
    val segmentCount: Int,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val errorCode: String?,
    val errorMessage: String?,
)

@Serializable
data class SentSmsListItemResponse(
    val messageId: Long,
    val to: String,
    val status: String,
    val createdAt: String,
    val sentAt: String?,
    val deliveredAt: String?,
    val failedAt: String?,
    val clientReference: String?,
    val segmentCount: Int,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val errorCode: String?,
)

@Serializable
data class ReceivedSmsListItemResponse(
    val smsId: Long,
    val from: String,
    val message: String,
    val receivedAt: String,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val forwardStatus: String,
    val retryCount: Int,
    val lastForwardAttemptAt: String?,
    val nextRetryAt: String?,
    val webhookResponseCode: Int?,
    val errorCode: String?,
    val errorMessage: String?,
)

@Serializable
data class ReceivedSmsDetailResponse(
    val smsId: Long,
    val from: String,
    val message: String,
    val receivedAt: String,
    val subscriptionId: Int?,
    val simSlot: Int?,
    val forwardStatus: String,
    val retryCount: Int,
    val lastForwardAttemptAt: String?,
    val nextRetryAt: String?,
    val webhookResponseCode: Int?,
    val webhookResponseBody: String?,
    val errorCode: String?,
    val errorMessage: String?,
    val attempts: List<WebhookAttemptSummaryResponse>,
)

@Serializable
data class WebhookAttemptResponse(
    val attemptId: Long,
    val smsId: Long,
    val attemptNumber: Int,
    val requestUrlSummary: String,
    val responseCode: Int?,
    val responseBodySummary: String?,
    val durationMs: Long?,
    val success: Boolean,
    val errorCode: String?,
    val errorMessage: String?,
    val attemptedAt: String,
)

@Serializable
data class WebhookAttemptSummaryResponse(
    val attemptId: Long,
    val attemptNumber: Int,
    val responseCode: Int?,
    val success: Boolean,
    val durationMs: Long?,
    val errorCode: String?,
    val attemptedAt: String,
)

@Serializable
data class WebhookTestResponse(
    val success: Boolean,
    val responseCode: Int?,
    val durationMs: Long,
    val responseBodySummary: String?,
    val errorCode: String?,
    val errorMessage: String?,
)

@Serializable
data class WebhookRetryAcceptedResponse(
    val smsId: Long,
    val forwardStatus: String,
    val retryCount: Int,
    val nextRetryAt: String?,
    val scheduled: Boolean,
)

@Serializable
data class FeatureNotReadyResponse(
    val feature: String,
    val message: String = "This endpoint is reserved for a later implementation phase.",
)
