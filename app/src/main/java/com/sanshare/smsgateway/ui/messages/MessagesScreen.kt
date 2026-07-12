package com.sanshare.smsgateway.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sanshare.smsgateway.ui.component.StatusCard

@Composable
fun MessagesRoute() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Messages", style = MaterialTheme.typography.headlineSmall)
        StatusCard(
            title = "Sent and received messages",
            value = "Pending implementation",
            detail = "Message storage, inbox, status APIs, and filters are added in later phases.",
        )
    }
}
