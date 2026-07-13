package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.sms.LowBatteryWebhookPayload
import com.sanshare.smsgateway.sms.WebhookDispatchRequest
import com.sanshare.smsgateway.sms.WebhookDispatchResult
import com.sanshare.smsgateway.sms.WebhookForwarder
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class RunLowBatteryWebhookUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val webhookForwarder: WebhookForwarder,
) {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
    }

    suspend operator fun invoke(
        batteryPercentage: Int,
        thresholdPercentage: Int,
        isCharging: Boolean,
    ): WebhookDispatchResult {
        val settings = settingsRepository.getSettings()
        val payload = LowBatteryWebhookPayload(
            eventType = "device.battery.low",
            deviceId = settings.deviceId,
            batteryPercentage = batteryPercentage,
            thresholdPercentage = thresholdPercentage,
            isCharging = isCharging,
            observedAt = Instant.now().toString(),
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
