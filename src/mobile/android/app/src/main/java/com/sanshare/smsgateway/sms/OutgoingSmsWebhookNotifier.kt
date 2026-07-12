package com.sanshare.smsgateway.sms

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class OutgoingSmsWebhookNotifier @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val webhookForwarder: WebhookForwarder,
    private val logger: AppLogger,
) {
    private val json = Json {
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun notifyStatus(entity: SentSmsEntity) {
        val settings = settingsRepository.getSettings()
        val payload = entity.toOutgoingWebhookPayload(settings.deviceId)
        val result = webhookForwarder.forward(
            WebhookDispatchRequest(
                eventType = payload.eventType,
                requestBody = json.encodeToString(payload),
                requestId = UUID.randomUUID().toString(),
            ),
        )

        if (!result.success && result.errorCode != "WEBHOOK_DISABLED" && result.errorCode != "WEBHOOK_NOT_CONFIGURED") {
            logger.warning(
                "WEBHOOK",
                "Outgoing webhook failed for messageId=${entity.id} status=${entity.status} error=${result.errorCode}",
            )
        }
    }
}
