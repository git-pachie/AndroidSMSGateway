package com.sanshare.smsgateway.domain.model

data class GatewayPermissionState(
    val canSendSms: Boolean = false,
    val canReceiveSms: Boolean = false,
    val canReadSms: Boolean = false,
    val canPostNotifications: Boolean = true,
    val batteryOptimizationIgnored: Boolean = false,
)
