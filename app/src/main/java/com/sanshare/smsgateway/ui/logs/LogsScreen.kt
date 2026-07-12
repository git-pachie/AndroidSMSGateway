package com.sanshare.smsgateway.ui.logs

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
fun LogsRoute() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Logs", style = MaterialTheme.typography.headlineSmall)
        StatusCard(
            title = "Operational logs",
            value = "Pending implementation",
            detail = "Database-backed system logs, audit logs, and webhook attempts are added in later phases.",
        )
    }
}
