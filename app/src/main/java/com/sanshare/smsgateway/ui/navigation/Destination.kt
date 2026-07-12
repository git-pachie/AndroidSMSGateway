package com.sanshare.smsgateway.ui.navigation

enum class Destination(
    val route: String,
    val label: String,
) {
    Dashboard("dashboard", "Dashboard"),
    Messages("messages", "Messages"),
    Logs("logs", "Logs"),
    Settings("settings", "Settings"),
}
