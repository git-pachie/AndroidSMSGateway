package com.sanshare.smsgateway.domain.model

data class AppSettings(
    val deviceId: String,
    val serverPort: Int,
    val apiKeyConfigured: Boolean,
    val apiKeyIdentifier: String?,
    val webhookUrl: String?,
    val webhookEnabled: Boolean,
    val webhookSecretConfigured: Boolean,
    val maxRetryCount: Int,
    val retryBaseDelaySeconds: Int,
    val allowedPrefixes: List<String>,
    val dailySmsLimitEnabled: Boolean,
    val dailySmsLimit: Int,
    val rateLimitPerMinute: Int,
    val autoStartEnabled: Boolean,
    val requireHttpsWebhook: Boolean,
    val serverEnabled: Boolean,
)
