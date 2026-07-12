package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.sms.WebhookDispatchRequest
import com.sanshare.smsgateway.sms.WebhookDispatchResult
import com.sanshare.smsgateway.sms.WebhookForwarder
import com.sanshare.smsgateway.sms.IncomingSmsWebhookPayload
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class RunWebhookTestUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val webhookForwarder: WebhookForwarder,
) {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
    }

    suspend operator fun invoke(): WebhookDispatchResult {
        val settings = settingsRepository.getSettings()
        val now = Instant.now()
        val id = now.toEpochMilli()
        val payload = IncomingSmsWebhookPayload(
            eventType = "sms.gateway.test",
            deviceId = settings.deviceId,
            from = "gateway-test",
            message = "Android SMS Gateway test event",
            receivedAt = now.toString(),
            subscriptionId = null,
            simSlot = null,
            smsId = id,
        )
        return webhookForwarder.forward(
            WebhookDispatchRequest(
                eventType = payload.eventType,
                requestBody = json.encodeToString(payload),
                requestId = UUID.randomUUID().toString(),
            ),
        )
    }
}
