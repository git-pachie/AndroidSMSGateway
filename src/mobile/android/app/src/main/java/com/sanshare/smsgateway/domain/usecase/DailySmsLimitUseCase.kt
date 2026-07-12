package com.sanshare.smsgateway.domain.usecase

import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import javax.inject.Inject

data class DailyLimitDecision(
    val allowed: Boolean,
    val used: Int,
    val limit: Int,
)

class DailySmsLimitUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sentSmsRepository: SentSmsRepository,
) {
    suspend fun canAccept(nowMillis: Long = System.currentTimeMillis()): DailyLimitDecision {
        val settings = settingsRepository.getSettings()
        val used = sentSmsRepository.countAcceptedTodayUtc(nowMillis)
        return if (!settings.dailySmsLimitEnabled) {
            DailyLimitDecision(true, used, settings.dailySmsLimit)
        } else {
            DailyLimitDecision(used < settings.dailySmsLimit, used, settings.dailySmsLimit)
        }
    }
}
