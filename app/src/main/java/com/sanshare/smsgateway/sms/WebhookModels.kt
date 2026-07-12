package com.sanshare.smsgateway.sms

import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class WebhookPayload(
    val eventType: String,
    val deviceId: String,
    val from: String,
    val message: String,
    val receivedAt: String,
    val subscriptionId: Int? = null,
    val simSlot: Int? = null,
    val smsId: Long,
)

data class WebhookDispatchRequest(
    val payload: WebhookPayload,
    val requestId: String,
)

data class WebhookDispatchResult(
    val success: Boolean,
    val responseCode: Int?,
    val responseBodySummary: String?,
    val durationMs: Long,
    val errorCode: String?,
    val errorMessage: String?,
    val retryable: Boolean,
    val requestUrlSummary: String?,
)

fun ReceivedSmsEntity.toWebhookPayload(deviceId: String): WebhookPayload {
    return WebhookPayload(
        eventType = "sms.received",
        deviceId = deviceId,
        from = fromNumber,
        message = message,
        receivedAt = Instant.ofEpochMilli(receivedAt).toString(),
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        smsId = id,
    )
}
