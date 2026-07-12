package com.sanshare.smsgateway.core.validation

import java.net.URI

data class ValidationResult(
    val valid: Boolean,
    val normalized: String? = null,
    val error: String? = null,
) {
    companion object {
        fun ok(normalized: String? = null) = ValidationResult(true, normalized)
        fun error(message: String) = ValidationResult(false, error = message)
    }
}

object GatewayValidators {
    fun serverPort(port: Int): ValidationResult {
        return if (port in 1024..65535) ValidationResult.ok(port.toString()) else ValidationResult.error("Port must be 1024 to 65535")
    }

    fun destinationNumber(raw: String): ValidationResult {
        val value = raw.trim()
        if (value.isBlank()) return ValidationResult.error("Destination is required")
        if (value.length > 32) return ValidationResult.error("Destination is too long")
        if (!Regex("^\\+?[0-9 .()\\-]{3,32}$").matches(value)) return ValidationResult.error("Destination has invalid characters")
        return ValidationResult.ok(value)
    }

    fun smsMessage(raw: String): ValidationResult {
        if (raw.isBlank()) return ValidationResult.error("Message is required")
        if (raw.length > 5_000) return ValidationResult.error("Message exceeds 5000 characters")
        return ValidationResult.ok(raw)
    }

    fun clientReference(raw: String?): ValidationResult {
        val value = raw?.trim().orEmpty()
        if (value.length > 200) return ValidationResult.error("Client reference exceeds 200 characters")
        return ValidationResult.ok(value.ifBlank { null })
    }

    fun webhookUrl(raw: String?, enabled: Boolean, requireHttps: Boolean): ValidationResult {
        val value = raw?.trim().orEmpty()
        if (enabled && value.isBlank()) return ValidationResult.error("Webhook URL is required when enabled")
        if (value.isBlank()) return ValidationResult.ok(null)
        val uri = runCatching { URI(value) }.getOrNull() ?: return ValidationResult.error("Webhook URL is invalid")
        val scheme = uri.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return ValidationResult.error("Webhook URL must use HTTP or HTTPS")
        if (requireHttps && scheme != "https") return ValidationResult.error("Webhook URL must use HTTPS")
        if (uri.host.isNullOrBlank()) return ValidationResult.error("Webhook URL must include a host")
        return ValidationResult.ok(value)
    }

    fun allowedPrefixes(raw: String?): List<String> {
        return raw.orEmpty()
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .filter { Regex("^\\+?[0-9]{1,15}$").matches(it) }
    }
}
