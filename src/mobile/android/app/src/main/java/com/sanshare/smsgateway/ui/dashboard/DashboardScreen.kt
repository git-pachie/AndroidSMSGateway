package com.sanshare.smsgateway.ui.dashboard

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sanshare.smsgateway.domain.model.GatewayPermissionState
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.ui.UiFormatters
import com.sanshare.smsgateway.ui.component.ConfirmationDialog
import com.sanshare.smsgateway.ui.component.PrimaryOperationButton
import com.sanshare.smsgateway.ui.component.SectionHeader
import com.sanshare.smsgateway.ui.component.SecondaryOperationButton
import com.sanshare.smsgateway.ui.component.StatusCard
import com.sanshare.smsgateway.ui.component.StatusChip
import com.sanshare.smsgateway.ui.component.WarningBanner
import com.sanshare.smsgateway.ui.theme.GatewayAmber
import com.sanshare.smsgateway.ui.theme.GatewayGreen
import com.sanshare.smsgateway.ui.theme.GatewayRed
import com.sanshare.smsgateway.util.BatteryOptimizationUtils

@Composable
fun DashboardRoute(
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenMessages: () -> Unit,
    onCopyText: (String, String) -> Unit,
    onShowSnackbar: suspend (String) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity

    val sendPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.stopGateway()
    }
    val receivePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.stopGateway()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.stopGateway()
    }

    DashboardScreen(
        uiState = uiState,
        onStart = viewModel::startGateway,
        onStop = viewModel::stopGateway,
        onTestWebhook = viewModel::testWebhook,
        onOpenSettings = onOpenSettings,
        onOpenLogs = onOpenLogs,
        onOpenMessages = onOpenMessages,
        onCopyApiUrl = {
            if (uiState.apiBaseUrl != "Unavailable") onCopyText("API URL", uiState.apiBaseUrl)
        },
        onRequestPermissions = {
            if (!uiState.permissions.canSendSms) {
                sendPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
            if (!uiState.permissions.canReceiveSms) {
                receivePermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !uiState.permissions.canPostNotifications) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenAppSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                },
            )
        },
        onOpenBatterySettings = {
            context.startActivity(BatteryOptimizationUtils.settingsIntent(context))
        },
    )
}

@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onTestWebhook: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenMessages: () -> Unit,
    onCopyApiUrl: () -> Unit,
    onRequestPermissions: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenBatterySettings: () -> Unit,
) {
    var showStopConfirmation by remember { mutableStateOf(false) }

    if (showStopConfirmation) {
        ConfirmationDialog(
            title = "Stop gateway",
            message = "Stop the foreground service and embedded API server?",
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
        uiState.errorMessage?.let { WarningBanner(it) }
        if (!uiState.permissions.canSendSms || !uiState.permissions.canReceiveSms || !uiState.permissions.canPostNotifications) {
            WarningBanner("The gateway is in degraded mode until SMS and notification permissions are granted.")
        }

        StatusCard(
            title = "Gateway Status",
            value = uiState.gatewayState.name,
            detail = "Uptime ${UiFormatters.duration(uiState.uptimeMillis)} • Device ${uiState.deviceId} • Port ${uiState.serverPort}",
            trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onCopyApiUrl, enabled = uiState.apiBaseUrl != "Unavailable") {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy API URL")
                    }
                    StatusChip(uiState.gatewayState.name, statusColor(uiState.gatewayState))
                }
            },
        )
        StatusCard(
            title = "API Base URL",
            value = uiState.apiBaseUrl,
            detail = "Use this LAN address for authenticated API access.",
        )

        SectionHeader("Device Readiness")
        PermissionSummary(uiState.permissions)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryOperationButton(
                text = "Request Permissions",
                onClick = onRequestPermissions,
                modifier = Modifier.weight(1f),
            )
            SecondaryOperationButton(
                text = "App Settings",
                onClick = onOpenAppSettings,
                modifier = Modifier.weight(1f),
            )
        }
        SecondaryOperationButton(
            text = if (uiState.permissions.batteryOptimizationIgnored) "Battery: Unrestricted" else "Open Battery Settings",
            onClick = onOpenBatterySettings,
            modifier = Modifier.fillMaxWidth(),
        )

        SectionHeader("Webhook")
        StatusCard(
            title = "Webhook Delivery",
            value = if (uiState.webhookEnabled) "Enabled" else "Disabled",
            detail = "URL ${UiFormatters.urlPreview(uiState.webhookUrlPreview)} • Pending ${uiState.pendingWebhook} • Failed ${uiState.failedWebhook} • Retry base ${uiState.effectiveRetryBaseDelaySeconds}s",
        )
        uiState.webhookTestStatus?.let {
            StatusCard(
                title = "Webhook Test",
                value = if (uiState.isTestingWebhook) "Running" else "Latest result",
                detail = it,
            )
        }

        SectionHeader("Today")
        StatusCard(
            title = "Message Totals",
            value = "${uiState.sentToday} sent • ${uiState.receivedToday} received",
            detail = "Outgoing failures ${uiState.failedOutgoing} • Webhook failures ${uiState.failedWebhook}",
        )

        SectionHeader("Recent Activity")
        StatusCard(
            title = "Last Events",
            value = "Last sent ${UiFormatters.relativeTime(uiState.lastSentSmsAt)}",
            detail = "Last received ${UiFormatters.relativeTime(uiState.lastIncomingSmsAt)} • Webhook success ${UiFormatters.relativeTime(uiState.lastWebhookSuccessAt)} • Webhook failure ${UiFormatters.relativeTime(uiState.lastWebhookFailureAt)}",
        )

        SectionHeader("Operations")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PrimaryOperationButton(
                text = "Start Gateway",
                onClick = onStart,
                enabled = uiState.gatewayState == GatewayServiceState.STOPPED || uiState.gatewayState == GatewayServiceState.ERROR,
                modifier = Modifier.weight(1f),
            )
            SecondaryOperationButton(
                text = "Stop Gateway",
                onClick = { showStopConfirmation = true },
                enabled = uiState.gatewayState == GatewayServiceState.RUNNING,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryOperationButton(text = "View Messages", onClick = onOpenMessages, modifier = Modifier.weight(1f))
            SecondaryOperationButton(text = "View Logs", onClick = onOpenLogs, modifier = Modifier.weight(1f))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryOperationButton(text = "Open Settings", onClick = onOpenSettings, modifier = Modifier.weight(1f))
            SecondaryOperationButton(
                text = if (uiState.isTestingWebhook) "Testing..." else "Test Webhook",
                onClick = onTestWebhook,
                enabled = !uiState.isTestingWebhook,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun PermissionSummary(permissionState: GatewayPermissionState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PermissionRow("SEND_SMS", permissionState.canSendSms)
        PermissionRow("RECEIVE_SMS", permissionState.canReceiveSms)
        PermissionRow("READ_SMS", permissionState.canReadSms)
        PermissionRow("POST_NOTIFICATIONS", permissionState.canPostNotifications)
        PermissionRow("Battery unrestricted", permissionState.batteryOptimizationIgnored)
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean) {
    StatusCard(
        title = label,
        value = if (granted) "Ready" else "Needs attention",
        detail = if (granted) "Operational requirement satisfied." else "Grant or adjust this setting for reliable gateway behavior.",
        trailing = {
            StatusChip(label = if (granted) "OK" else "Missing", containerColor = if (granted) GatewayGreen else GatewayAmber)
        },
    )
}

private fun statusColor(state: GatewayServiceState) = when (state) {
    GatewayServiceState.RUNNING -> GatewayGreen
    GatewayServiceState.ERROR -> GatewayRed
    GatewayServiceState.STARTING,
    GatewayServiceState.STOPPING,
    GatewayServiceState.STOPPED,
    -> GatewayAmber
}
