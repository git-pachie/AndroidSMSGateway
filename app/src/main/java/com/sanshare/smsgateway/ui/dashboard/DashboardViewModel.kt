package com.sanshare.smsgateway.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.service.SmsGatewayService
import com.sanshare.smsgateway.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    application: Application,
    gatewayStateRepository: GatewayStateRepository,
    settingsRepository: SettingsRepository,
    sentSmsRepository: SentSmsRepository,
    receivedSmsRepository: ReceivedSmsRepository,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext

    init {
        viewModelScope.launch {
            settingsRepository.ensureInitialized()
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        gatewayStateRepository.state,
        settingsRepository.observeSettings(),
        sentSmsRepository.countByStatus("FAILED"),
        receivedSmsRepository.countByForwardStatus("PENDING"),
        receivedSmsRepository.countByForwardStatus("FAILED"),
    ) { state, settings, failedOutgoing, pendingWebhook, failedWebhook ->
            val now = System.currentTimeMillis()
            DashboardUiState(
                gatewayState = state,
                deviceId = settings?.deviceId ?: AppConstants.DEFAULT_DEVICE_ID,
                serverPort = settings?.serverPort ?: AppConstants.DEFAULT_SERVER_PORT,
                permissions = PermissionUtils.gatewayPermissionState(appContext),
                sentToday = sentSmsRepository.countAcceptedTodayUtc(now),
                receivedToday = receivedSmsRepository.countReceivedTodayUtc(now),
                failedOutgoing = failedOutgoing,
                pendingWebhook = pendingWebhook,
                failedWebhook = failedWebhook,
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    fun startGateway() {
        val intent = SmsGatewayService.startIntent(appContext)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(appContext, intent)
        } else {
            appContext.startService(intent)
        }
    }

    fun stopGateway() {
        appContext.startService(SmsGatewayService.stopIntent(appContext))
    }

}
