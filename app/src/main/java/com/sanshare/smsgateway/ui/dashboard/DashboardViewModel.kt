package com.sanshare.smsgateway.ui.dashboard

import android.app.Application
import android.content.Context
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.core.network.NetworkAddressProvider
import com.sanshare.smsgateway.data.local.entity.ReceivedSmsEntity
import com.sanshare.smsgateway.data.local.entity.SentSmsEntity
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.domain.model.OutgoingSmsStatus
import com.sanshare.smsgateway.domain.repository.GatewayStateRepository
import com.sanshare.smsgateway.domain.repository.ReceivedSmsQuery
import com.sanshare.smsgateway.domain.repository.ReceivedSmsRepository
import com.sanshare.smsgateway.domain.repository.SentSmsQuery
import com.sanshare.smsgateway.domain.repository.SentSmsRepository
import com.sanshare.smsgateway.domain.repository.SettingsRepository
import com.sanshare.smsgateway.domain.repository.WebhookAttemptRepository
import com.sanshare.smsgateway.domain.usecase.RunWebhookTestUseCase
import com.sanshare.smsgateway.service.SmsGatewayService
import com.sanshare.smsgateway.sms.WebhookRetryPolicy
import com.sanshare.smsgateway.util.PermissionUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    webhookAttemptRepository: WebhookAttemptRepository,
    private val networkAddressProvider: NetworkAddressProvider,
    private val runWebhookTestUseCase: RunWebhookTestUseCase,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val sentTodayFlow = MutableStateFlow(0)
    private val receivedTodayFlow = MutableStateFlow(0)
    private val isTestingWebhook = MutableStateFlow(false)
    private val webhookTestStatus = MutableStateFlow<String?>(null)
    private val runningSinceMillis = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            settingsRepository.ensureInitialized()
            sentTodayFlow.value = sentSmsRepository.countAcceptedTodayUtc()
            receivedTodayFlow.value = receivedSmsRepository.countReceivedTodayUtc()
        }
        viewModelScope.launch {
            gatewayStateRepository.state.collect { state ->
                if (state == GatewayServiceState.RUNNING && runningSinceMillis.value == null) {
                    runningSinceMillis.value = System.currentTimeMillis()
                } else if (state != GatewayServiceState.RUNNING) {
                    runningSinceMillis.value = null
                }
            }
        }
    }

    private val outgoingMetrics = combine(
        sentSmsRepository.countByStatus(OutgoingSmsStatus.PENDING),
        sentSmsRepository.countByStatus(OutgoingSmsStatus.SENDING),
        sentSmsRepository.countByStatus(OutgoingSmsStatus.SENT),
        sentSmsRepository.countByStatus(OutgoingSmsStatus.DELIVERED),
        sentSmsRepository.countByStatus(OutgoingSmsStatus.FAILED),
    ) { pendingOutgoing, sendingOutgoing, sentOutgoing, deliveredOutgoing, failedOutgoing ->
        OutgoingDashboardMetrics(
            pendingOutgoing = pendingOutgoing,
            sendingOutgoing = sendingOutgoing,
            sentOutgoing = sentOutgoing,
            deliveredOutgoing = deliveredOutgoing,
            failedOutgoing = failedOutgoing,
        )
    }

    private val webhookMetrics = combine(
        receivedSmsRepository.countByForwardStatus("PENDING"),
        receivedSmsRepository.countByForwardStatus("FAILED"),
    ) { pendingWebhook, failedWebhook ->
        WebhookDashboardMetrics(
            pendingWebhook = pendingWebhook,
            failedWebhook = failedWebhook,
        )
    }

    private val appMetrics = combine(
        sentTodayFlow,
        receivedTodayFlow,
        outgoingMetrics,
        webhookMetrics,
    ) { sentToday, receivedToday, outgoingMetrics, webhookMetrics ->
        AppDashboardMetrics(
            sentToday = sentToday,
            receivedToday = receivedToday,
            outgoingMetrics = outgoingMetrics,
            webhookMetrics = webhookMetrics,
        )
    }

    private val webhookHistory = combine(
        webhookAttemptRepository.observeLatestAttemptedAt(success = true),
        webhookAttemptRepository.observeLatestAttemptedAt(success = false),
    ) { successAt, failedAt ->
        successAt to failedAt
    }

    private val recentActivity = combine(
        gatewayStateRepository.state,
        runningSinceMillis,
    ) { _, _ ->
        RecentDashboardActivity(
            lastSent = sentSmsRepository.query(SentSmsQuery(limit = 1)).items.firstOrNull(),
            lastReceived = receivedSmsRepository.query(ReceivedSmsQuery(limit = 1)).items.firstOrNull(),
        )
    }

    private val dashboardChrome = combine(
        webhookHistory,
        isTestingWebhook,
        webhookTestStatus,
    ) { webhookHistory, isTestingWebhook, webhookTestStatus ->
        DashboardChromeState(
            webhookHistory = webhookHistory,
            isTestingWebhook = isTestingWebhook,
            webhookTestStatus = webhookTestStatus,
        )
    }

    private val dashboardIdentity = combine(
        gatewayStateRepository.state,
        settingsRepository.observeSettings(),
        runningSinceMillis,
    ) { state, settings, runningSinceMillis ->
        DashboardIdentityState(
            state = state,
            settings = settings,
            runningSinceMillis = runningSinceMillis,
        )
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        dashboardIdentity,
        appMetrics,
        dashboardChrome,
        recentActivity,
    ) { identity, appMetrics, chrome, recentActivity ->
            val configuredPort = identity.settings?.serverPort ?: AppConstants.DEFAULT_SERVER_PORT
            val baseUrl = when (identity.state) {
                GatewayServiceState.RUNNING,
                GatewayServiceState.STARTING,
                -> networkAddressProvider.baseUrl(configuredPort)
                else -> null
            }
            DashboardUiState(
                gatewayState = identity.state,
                isLoading = identity.settings == null,
                deviceId = identity.settings?.deviceId ?: AppConstants.DEFAULT_DEVICE_ID,
                serverPort = configuredPort,
                deviceAddress = baseUrl ?: "Unavailable",
                apiBaseUrl = baseUrl ?: "Unavailable",
                permissions = PermissionUtils.gatewayPermissionState(appContext),
                uptimeMillis = identity.runningSinceMillis?.let { System.currentTimeMillis() - it },
                sentToday = appMetrics.sentToday,
                receivedToday = appMetrics.receivedToday,
                pendingOutgoing = appMetrics.outgoingMetrics.pendingOutgoing,
                sendingOutgoing = appMetrics.outgoingMetrics.sendingOutgoing,
                sentOutgoing = appMetrics.outgoingMetrics.sentOutgoing,
                deliveredOutgoing = appMetrics.outgoingMetrics.deliveredOutgoing,
                failedOutgoing = appMetrics.outgoingMetrics.failedOutgoing,
                pendingWebhook = appMetrics.webhookMetrics.pendingWebhook,
                failedWebhook = appMetrics.webhookMetrics.failedWebhook,
                webhookEnabled = identity.settings?.webhookEnabled == true,
                webhookUrlPreview = identity.settings?.webhookUrl,
                effectiveRetryBaseDelaySeconds = WebhookRetryPolicy.effectiveBaseDelaySeconds(identity.settings?.retryBaseDelaySeconds ?: 30),
                lastWebhookSuccessAt = chrome.webhookHistory.first,
                lastWebhookFailureAt = chrome.webhookHistory.second,
                lastIncomingSmsAt = recentActivity.lastReceived?.receivedAt,
                lastSentSmsAt = recentActivity.lastSent?.createdAt,
                isTestingWebhook = chrome.isTestingWebhook,
                webhookTestStatus = chrome.webhookTestStatus,
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

    fun testWebhook() {
        if (isTestingWebhook.value) return
        viewModelScope.launch {
            isTestingWebhook.value = true
            val result = runWebhookTestUseCase()
            webhookTestStatus.value = if (result.success) {
                "Webhook test succeeded with HTTP ${result.responseCode ?: 200}"
            } else {
                "${result.errorCode ?: "WEBHOOK_HTTP_ERROR"}: ${result.errorMessage ?: "Webhook test failed"}"
            }
            isTestingWebhook.value = false
        }
    }

}

private data class OutgoingDashboardMetrics(
    val pendingOutgoing: Int,
    val sendingOutgoing: Int,
    val sentOutgoing: Int,
    val deliveredOutgoing: Int,
    val failedOutgoing: Int,
)

private data class WebhookDashboardMetrics(
    val pendingWebhook: Int,
    val failedWebhook: Int,
)

private data class AppDashboardMetrics(
    val sentToday: Int,
    val receivedToday: Int,
    val outgoingMetrics: OutgoingDashboardMetrics,
    val webhookMetrics: WebhookDashboardMetrics,
)

private data class DashboardChromeState(
    val webhookHistory: Pair<Long?, Long?>,
    val isTestingWebhook: Boolean,
    val webhookTestStatus: String?,
)

private data class RecentDashboardActivity(
    val lastSent: SentSmsEntity?,
    val lastReceived: ReceivedSmsEntity?,
)

private data class DashboardIdentityState(
    val state: GatewayServiceState,
    val settings: com.sanshare.smsgateway.domain.model.AppSettings?,
    val runningSinceMillis: Long?,
)
