package com.sanshare.smsgateway.ui.dashboard

import com.sanshare.smsgateway.core.constants.AppConstants
import com.sanshare.smsgateway.domain.model.GatewayPermissionState
import com.sanshare.smsgateway.domain.model.GatewayServiceState

data class DashboardUiState(
    val gatewayState: GatewayServiceState = GatewayServiceState.STOPPED,
    val deviceId: String = AppConstants.DEFAULT_DEVICE_ID,
    val serverPort: Int = AppConstants.DEFAULT_SERVER_PORT,
    val deviceAddress: String = "Server pending",
    val permissions: GatewayPermissionState = GatewayPermissionState(),
    val sentToday: Int = 0,
    val receivedToday: Int = 0,
    val failedOutgoing: Int = 0,
    val pendingWebhook: Int = 0,
    val failedWebhook: Int = 0,
)
