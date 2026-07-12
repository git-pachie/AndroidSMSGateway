package com.sanshare.smsgateway.sms

import kotlin.math.pow

object WebhookRetryPolicy {
    private const val WORK_MANAGER_MIN_BACKOFF_SECONDS = 10

    fun minBackoffSeconds(): Int = WORK_MANAGER_MIN_BACKOFF_SECONDS

    fun effectiveBaseDelaySeconds(configuredSeconds: Int): Int {
        return configuredSeconds.coerceAtLeast(WORK_MANAGER_MIN_BACKOFF_SECONDS)
    }

    fun nextRetryDelayMillis(baseDelaySeconds: Int, retryCount: Int): Long {
        val exponent = (retryCount - 1).coerceAtLeast(0)
        val multiplier = 2.0.pow(exponent.toDouble()).toLong().coerceAtLeast(1L)
        return effectiveBaseDelaySeconds(baseDelaySeconds).toLong() * 1_000L * multiplier
    }
}
