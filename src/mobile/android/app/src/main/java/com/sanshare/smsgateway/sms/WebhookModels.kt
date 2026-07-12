package com.sanshare.smsgateway.sms

import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import java.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class IncomingSmsWebhookPayload(
    val eventType: String,
    val deviceId: String,
    val from: String,
    val message: String,
    val receivedAt: String,
    val subscriptionId: Int? = null,
    val simSlot: Int? = null,
    val smsId: Long,
)

@Serializable
data class OutgoingSmsWebhookPayload(
    val eventType: String,
    val deviceId: String,
    val messageId: Long,
    val to: String,
    val message: String,
    val status: String,
    val createdAt: String,
    val sendingAt: String? = null,
    val sentAt: String? = null,
    val deliveredAt: String? = null,
    val failedAt: String? = null,
    val clientReference: String? = null,
    val segmentCount: Int,
    val subscriptionId: Int? = null,
    val simSlot: Int? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
)

data class WebhookDispatchRequest(
    val eventType: String,
    val requestBody: String,
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

fun ReceivedSmsEntity.toWebhookPayload(deviceId: String): IncomingSmsWebhookPayload {
    return IncomingSmsWebhookPayload(
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

fun SentSmsEntity.toOutgoingWebhookPayload(deviceId: String): OutgoingSmsWebhookPayload {
    val webhookStatus = when (status) {
        "SENDING" -> "PENDING"
        else -> status
    }
    val eventType = when (webhookStatus) {
        "PENDING" -> "sms.outgoing.pending"
        "SENT" -> "sms.outgoing.sent"
        "FAILED" -> "sms.outgoing.failed"
        "DELIVERED" -> "sms.outgoing.delivered"
        else -> "sms.outgoing.updated"
    }
    return OutgoingSmsWebhookPayload(
        eventType = eventType,
        deviceId = deviceId,
        messageId = id,
        to = toNumber,
        message = message,
        status = webhookStatus,
        createdAt = Instant.ofEpochMilli(createdAt).toString(),
        sendingAt = sendingAt?.let { Instant.ofEpochMilli(it).toString() },
        sentAt = sentAt?.let { Instant.ofEpochMilli(it).toString() },
        deliveredAt = deliveredAt?.let { Instant.ofEpochMilli(it).toString() },
        failedAt = failedAt?.let { Instant.ofEpochMilli(it).toString() },
        clientReference = clientReference,
        segmentCount = segmentCount,
        subscriptionId = subscriptionId,
        simSlot = simSlot,
        errorCode = errorCode,
        errorMessage = errorMessage,
    )
}
