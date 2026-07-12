package com.sanshare.smsgateway.sms

import org.junit.Assert.assertEquals
import org.junit.Test

class WebhookRetryPolicyTest {
    @Test
    fun clampsBackoffToWorkManagerMinimum() {
        assertEquals(10, WebhookRetryPolicy.effectiveBaseDelaySeconds(1))
    }

    @Test
    fun calculatesExponentialRetryDelay() {
        assertEquals(30_000L, WebhookRetryPolicy.nextRetryDelayMillis(baseDelaySeconds = 30, retryCount = 1))
        assertEquals(60_000L, WebhookRetryPolicy.nextRetryDelayMillis(baseDelaySeconds = 30, retryCount = 2))
        assertEquals(120_000L, WebhookRetryPolicy.nextRetryDelayMillis(baseDelaySeconds = 30, retryCount = 3))
    }

    @Test
    fun buildsStableUniqueWorkName() {
        assertEquals("webhook-retry-9001", WorkManagerWebhookScheduler.uniqueWorkName(9001))
    }
}
