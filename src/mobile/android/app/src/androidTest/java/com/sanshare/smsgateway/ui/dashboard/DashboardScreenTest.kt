package com.sanshare.smsgateway.ui.dashboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sanshare.smsgateway.domain.model.GatewayPermissionState
import com.sanshare.smsgateway.domain.model.GatewayServiceState
import com.sanshare.smsgateway.ui.theme.AndroidSmsGatewayTheme
import org.junit.Rule
import org.junit.Test

class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersRunningState() {
        composeRule.setContent {
            AndroidSmsGatewayTheme {
                DashboardScreen(
                    uiState = DashboardUiState(
                        isLoading = false,
                        gatewayState = GatewayServiceState.RUNNING,
                        apiBaseUrl = "http://192.168.1.50:8080",
                        deviceId = "gateway-01",
                        permissions = GatewayPermissionState(
                            canSendSms = true,
                            canReceiveSms = true,
                            canReadSms = true,
                            canPostNotifications = true,
                            batteryOptimizationIgnored = true,
                        ),
                    ),
                    onStart = {},
                    onStop = {},
                    onTestWebhook = {},
                    onOpenSettings = {},
                    onOpenLogs = {},
                    onOpenMessages = {},
                    onCopyApiUrl = {},
                    onRequestPermissions = {},
                    onOpenAppSettings = {},
                    onOpenBatterySettings = {},
                )
            }
        }

        composeRule.onNodeWithText("Gateway Status").assertIsDisplayed()
        composeRule.onNodeWithText("RUNNING").assertIsDisplayed()
        composeRule.onNodeWithText("API Base URL").assertIsDisplayed()
    }
}
