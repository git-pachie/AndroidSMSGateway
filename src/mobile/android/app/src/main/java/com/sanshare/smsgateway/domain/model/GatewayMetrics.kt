package com.sanshare.smsgateway.domain.model

data class GatewayMetrics(
    val sentToday: Int = 0,
    val receivedToday: Int = 0,
    val failedOutgoing: Int = 0,
    val pendingWebhook: Int = 0,
    val failedWebhook: Int = 0,
)
