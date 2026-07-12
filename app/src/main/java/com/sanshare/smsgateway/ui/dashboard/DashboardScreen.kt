package com.sanshare.smsgateway.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanshare.smsgateway.domain.model.GatewayPermissionState
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.ui.component.ConfirmationDialog
import com.sanshare.smsgateway.ui.component.PrimaryOperationButton
import com.sanshare.smsgateway.ui.component.SecondaryOperationButton
import com.sanshare.smsgateway.ui.component.SectionHeader
import com.sanshare.smsgateway.ui.component.StatusCard
import com.sanshare.smsgateway.ui.component.StatusChip
import com.sanshare.smsgateway.ui.component.WarningBanner
import com.sanshare.smsgateway.ui.theme.GatewayAmber
import com.sanshare.smsgateway.ui.theme.GatewayGreen
import com.sanshare.smsgateway.ui.theme.GatewayRed

@Composable
fun DashboardRoute(
    onOpenSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState,
        onStart = viewModel::startGateway,
        onStop = viewModel::stopGateway,
        onOpenSettings = onOpenSettings,
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    var showStopConfirmation by remember { mutableStateOf(false) }

    if (showStopConfirmation) {
        ConfirmationDialog(
            title = "Stop gateway",
            message = "Stop the foreground service shell? HTTP server setup will be added in a later phase.",
            confirmText = "Stop",
            onConfirm = {
                showStopConfirmation = false
                onStop()
            },
            onDismiss = { showStopConfirmation = false },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Android SMS Gateway", style = MaterialTheme.typography.headlineSmall)
        WarningBanner("HTTP server implementation is pending. This phase starts only the Android foreground service shell.")

        StatusCard(
            title = "Gateway status",
            value = uiState.gatewayState.name,
            detail = "Device ${uiState.deviceId} on port ${uiState.serverPort}",
            trailing = {
                StatusChip(
                    label = uiState.gatewayState.name,
                    containerColor = statusColor(uiState.gatewayState),
                )
            },
        )

        StatusCard(
            title = "Network",
            value = uiState.deviceAddress,
            detail = "The device IP and API base URL are resolved when the HTTP server phase is implemented.",
        )

        SectionHeader("Metrics")
        StatusCard(
            title = "Message activity",
            value = "${uiState.sentToday} sent today, ${uiState.receivedToday} received today",
            detail = "${uiState.failedOutgoing} failed outgoing, ${uiState.pendingWebhook} pending webhooks, ${uiState.failedWebhook} failed webhooks",
        )

        SectionHeader("Permissions")
        PermissionSummary(uiState.permissions)

        SectionHeader("Operations")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PrimaryOperationButton(
                text = "Start Gateway",
                onClick = onStart,
                enabled = uiState.gatewayState != GatewayServiceState.RUNNING,
                modifier = Modifier.weight(1f),
            )
            SecondaryOperationButton(
                text = "Stop Gateway",
                onClick = { showStopConfirmation = true },
                enabled = uiState.gatewayState == GatewayServiceState.RUNNING,
                modifier = Modifier.weight(1f),
            )
        }
        SecondaryOperationButton(
            text = "Open Settings",
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun PermissionSummary(permissionState: GatewayPermissionState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionRow("SEND_SMS", permissionState.canSendSms)
        PermissionRow("RECEIVE_SMS", permissionState.canReceiveSms)
        PermissionRow("POST_NOTIFICATIONS", permissionState.canPostNotifications)
        PermissionRow("Battery optimization ignored", permissionState.batteryOptimizationIgnored)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    StatusCard(
        title = label,
        value = if (granted) "Ready" else "Needs attention",
        detail = if (granted) "Permission or setting is available." else "Request flow is added in the UI phase.",
        trailing = {
            StatusChip(
                label = if (granted) "OK" else "Missing",
                containerColor = if (granted) GatewayGreen else GatewayAmber,
            )
        },
    )
}

private fun statusColor(state: GatewayServiceState): Color {
    return when (state) {
        GatewayServiceState.RUNNING -> GatewayGreen
        GatewayServiceState.STARTING,
        GatewayServiceState.STOPPING,
        -> GatewayAmber
        GatewayServiceState.ERROR -> GatewayRed
        GatewayServiceState.STOPPED -> GatewayAmber
    }
}
