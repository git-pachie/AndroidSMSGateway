package com.sanshare.smsgateway.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.automirrored.outlined.Subject
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

enum class Destination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Dashboard("dashboard", "Dashboard", Icons.Outlined.Dashboard),
    Messages("messages", "Messages", Icons.AutoMirrored.Outlined.ListAlt),
    Logs("logs", "Logs", Icons.AutoMirrored.Outlined.Subject),
    Settings("settings", "Settings", Icons.Outlined.Settings),
}
