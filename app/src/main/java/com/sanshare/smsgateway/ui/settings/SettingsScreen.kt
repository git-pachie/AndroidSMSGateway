package com.sanshare.smsgateway.ui.settings

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
fun SettingsRoute() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineSmall)
        StatusCard(
            title = "Gateway settings",
            value = "Pending implementation",
            detail = "Device ID, server port, API key, SMS limits, and webhook settings are added in later phases.",
        )
    }
}
