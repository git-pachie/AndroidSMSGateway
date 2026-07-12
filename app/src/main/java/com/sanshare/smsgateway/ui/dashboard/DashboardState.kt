package com.sanshare.smsgateway.ui.dashboard

import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.domain.model.GatewayPermissionState
import com.sanshare.smsgateway.domain.model.GatewayServiceState

data class DashboardUiState(
    val isLoading: Boolean = true,
    val gatewayState: GatewayServiceState = GatewayServiceState.STOPPED,
    val deviceId: String = AppConstants.DEFAULT_DEVICE_ID,
    val serverPort: Int = AppConstants.DEFAULT_SERVER_PORT,
    val deviceAddress: String = "Server pending",
    val apiBaseUrl: String = "Unavailable",
    val permissions: GatewayPermissionState = GatewayPermissionState(),
    val uptimeMillis: Long? = null,
    val sentToday: Int = 0,
    val receivedToday: Int = 0,
    val pendingOutgoing: Int = 0,
    val sendingOutgoing: Int = 0,
    val sentOutgoing: Int = 0,
    val deliveredOutgoing: Int = 0,
    val failedOutgoing: Int = 0,
    val pendingWebhook: Int = 0,
    val failedWebhook: Int = 0,
    val webhookEnabled: Boolean = false,
    val webhookUrlPreview: String? = null,
    val effectiveRetryBaseDelaySeconds: Int = 30,
    val lastWebhookSuccessAt: Long? = null,
    val lastWebhookFailureAt: Long? = null,
    val lastIncomingSmsAt: Long? = null,
    val lastSentSmsAt: Long? = null,
    val errorMessage: String? = null,
    val isTestingWebhook: Boolean = false,
    val webhookTestStatus: String? = null,
)
