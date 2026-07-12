package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.sms.WebhookDispatchRequest
import com.sanshare.smsgateway.sms.WebhookDispatchResult
import com.sanshare.smsgateway.sms.WebhookForwarder
import com.sanshare.smsgateway.sms.WebhookPayload
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RunWebhookTestUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val webhookForwarder: WebhookForwarder,
) {
    suspend operator fun invoke(): WebhookDispatchResult {
        val settings = settingsRepository.getSettings()
        val now = Instant.now()
        val id = now.toEpochMilli()
        return webhookForwarder.forward(
            WebhookDispatchRequest(
                payload = WebhookPayload(
                    eventType = "sms.gateway.test",
                    deviceId = settings.deviceId,
                    from = "gateway-test",
                    message = "Android SMS Gateway test event",
                    receivedAt = now.toString(),
                    subscriptionId = null,
                    simSlot = null,
                    smsId = id,
                ),
                requestId = UUID.randomUUID().toString(),
            ),
        )
    }
}
