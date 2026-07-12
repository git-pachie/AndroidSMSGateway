package com.sanshare.smsgateway.core.security

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil

data class RateLimitDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long,
    val remaining: Int,
)

class RateLimiter(
    private val clockMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    private val buckets = ConcurrentHashMap<String, MutableList<Long>>()

    fun check(key: String, limitPerMinute: Int): RateLimitDecision {
        require(limitPerMinute > 0) { "limitPerMinute must be positive" }
        val now = clockMillis()
        val windowStart = now - WINDOW_MILLIS
        val bucket = buckets.computeIfAbsent(key) { mutableListOf() }
        synchronized(bucket) {
            bucket.removeAll { it < windowStart }
            if (bucket.size >= limitPerMinute) {
                val oldest = bucket.minOrNull() ?: now
                val retryAfter = ceil(((oldest + WINDOW_MILLIS) - now).coerceAtLeast(1).toDouble() / 1000.0).toLong()
                return RateLimitDecision(false, retryAfter, 0)
            }
            bucket += now
            return RateLimitDecision(true, 0, (limitPerMinute - bucket.size).coerceAtLeast(0))
        }
    }

    companion object {
        private const val WINDOW_MILLIS = 60_000L
        const val GLOBAL_SEND_KEY = "send:global"
    }
}
