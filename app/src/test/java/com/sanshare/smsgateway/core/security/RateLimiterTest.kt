package com.sanshare.smsgateway.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {
    @Test
    fun blocksAfterLimitUntilWindowExpires() {
        var now = 1_000L
        val limiter = RateLimiter { now }

        assertTrue(limiter.check("remote", 2).allowed)
        assertTrue(limiter.check("remote", 2).allowed)
        val blocked = limiter.check("remote", 2)

        assertFalse(blocked.allowed)
        assertTrue(blocked.retryAfterSeconds > 0)

        now += 60_001L
        assertTrue(limiter.check("remote", 2).allowed)
    }
}
