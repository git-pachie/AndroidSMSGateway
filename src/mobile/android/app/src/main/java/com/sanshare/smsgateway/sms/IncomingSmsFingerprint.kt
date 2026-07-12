package com.sanshare.smsgateway.sms

import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IncomingSmsFingerprint @Inject constructor() {
    fun create(sender: String, message: String, receivedAt: Long, subscriptionId: Int?): String {
        val normalizedSender = normalizeSender(sender)
        val bucket = receivedAt / DUPLICATE_WINDOW_MILLIS
        val raw = buildString {
            append(normalizedSender)
            append('|')
            append(bucket)
            append('|')
            append(message.trim())
            append('|')
            append(subscriptionId ?: "none")
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }

    fun normalizeSender(sender: String): String {
        val trimmed = sender.trim()
        return if (trimmed.startsWith("+") || trimmed.any(Char::isDigit)) {
            buildString {
                trimmed.forEachIndexed { index, ch ->
                    if (ch.isDigit() || (ch == '+' && index == 0)) append(ch)
                }
            }.ifBlank { trimmed.uppercase() }
        } else {
            trimmed.uppercase()
        }
    }

    companion object {
        const val DUPLICATE_WINDOW_MILLIS = 5 * 60 * 1000L
    }
}
