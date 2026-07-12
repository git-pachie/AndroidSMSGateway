package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.core.logging.AppLogger
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.service.GatewayServiceStarter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HandleBootCompletedUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val gatewayServiceStarter: GatewayServiceStarter,
    private val logger: AppLogger,
) {
    suspend operator fun invoke(): Boolean {
        val settings = settingsRepository.getSettings()
        if (!settings.autoStartEnabled) {
            logger.info("Boot", "Auto-start disabled, boot receiver exiting")
            return false
        }
        gatewayServiceStarter.start()
        logger.info("Boot", "Gateway auto-start requested after boot")
        return true
    }
}
