package com.sanshare.smsgateway.ui.messages

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sanshare.smsgateway.ui.theme.AndroidSmsGatewayTheme
import org.junit.Rule
import org.junit.Test

class MessagesScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersEmptySentAndReceivedStates() {
        composeRule.setContent {
            AndroidSmsGatewayTheme {
                MessagesScreen(
                    uiState = MessagesUiState(),
                    onSelectTab = {},
                    onDestinationChange = {},
                    onMessageChange = {},
                    onClientReferenceChange = {},
                    onSubscriptionIdChange = {},
                    onSend = {},
                    onRequestSendPermission = {},
                    onRequestReceivePermission = {},
                    onSearchFilterChange = {},
                    onStatusFilterChange = {},
                    onToggleSortDirection = {},
                    onPreviousPage = {},
                    onNextPage = {},
                    onSelectSentMessage = {},
                    onClearSelectedSentMessage = {},
                    onSelectReceivedMessage = {},
                    onClearSelectedReceivedMessage = {},
                    onRetrySelectedReceivedMessage = {},
                    onCopyText = { _, _ -> },
                )
            }
        }

        composeRule.onNodeWithText("Sent Messages").assertIsDisplayed()
        composeRule.onNodeWithText("No sent messages").assertIsDisplayed()
    }
}
